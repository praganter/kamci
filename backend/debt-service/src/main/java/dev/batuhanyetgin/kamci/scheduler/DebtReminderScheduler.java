package dev.batuhanyetgin.kamci.scheduler;

import dev.batuhanyetgin.commonmodule.event.DebtReminderEvent;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.kamci.document.DebtDocument;
import dev.batuhanyetgin.kamci.enums.DebtStatus;
import dev.batuhanyetgin.kamci.enums.PaymentStatus;
import dev.batuhanyetgin.kamci.producer.DebtEventProducer;
import dev.batuhanyetgin.kamci.repositoryservice.DebtRepositoryService;
import dev.batuhanyetgin.kamci.util.PaymentPeriodUtil;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class DebtReminderScheduler {

    private final DebtRepositoryService debtRS;
    private final DebtEventProducer debtEventProducer;
    private final Executor taskExecutor;
    private final Clock clock;

    @Autowired
    public DebtReminderScheduler(final DebtRepositoryService debtRS,
                                 final DebtEventProducer debtEventProducer,
                                 @Qualifier("taskExecutor") final Executor taskExecutor) {
        this(debtRS, debtEventProducer, taskExecutor, Clock.systemDefaultZone());
    }

    DebtReminderScheduler(final DebtRepositoryService debtRS,
                          final DebtEventProducer debtEventProducer,
                          final Executor taskExecutor,
                          final Clock clock) {
        this.debtRS = debtRS;
        this.debtEventProducer = debtEventProducer;
        this.taskExecutor = taskExecutor;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0/15 * * * *")
    @SchedulerLock(name = "debtReminderScheduler", lockAtMostFor = "PT13M", lockAtLeastFor = "PT10M")
    public void processReminders() {
        final LocalDateTime now = LocalDateTime.now(clock);
        log.info("Processing debt reminders/auto-payments at: {}", now);

        processStatusResetBatch(debtRS.findActiveDebts(), now);
        processReminderBatch(debtRS.findDueReminders(now), now);
        processAutoPaymentBatch(debtRS.findDueAutoPayments(now), now);
        processOverdueBatch(debtRS.findUnpaidManualDueDebts(now), now);
    }

    private void processStatusResetBatch(final List<DebtDocument> debts, final LocalDateTime now) {
        if (debts.isEmpty()) {
            return;
        }

        final AtomicInteger resetCount = new AtomicInteger();
        debts.forEach(debt -> {
            if (shouldResetPaymentStatus(debt, now)) {
                debt.setPaymentStatus(PaymentStatus.UNPAID);
                try {
                    debtRS.update(debt);
                    resetCount.incrementAndGet();
                } catch (Exception e) {
                    log.error("Failed to reset payment status for debt {}: {}", debt.getId(), e.getMessage());
                }
            }
        });
        if (resetCount.get() > 0) {
            log.info("Reset payment status to UNPAID for {} ACTIVE debt(s) due to new period", resetCount.get());
        }
    }

    private boolean shouldResetPaymentStatus(final DebtDocument debt, final LocalDateTime now) {
        if (debt.getPaymentStatus() == PaymentStatus.UNPAID) {
            return false;
        }

        final LocalDateTime periodStart = PaymentPeriodUtil.getPeriodStart(
                debt.getFirstPaymentDate(), debt.getPaymentPeriodType(), now);

        // If last payment/skip was in a previous period, reset to UNPAID for current period
        return debt.getPayments().stream()
                .noneMatch(p -> p.getPaidTime() != null && !p.getPaidTime().isBefore(periodStart));
    }

    private void processReminderBatch(final List<DebtDocument> debts, final LocalDateTime now) {
        if (debts.isEmpty()) {
            return;
        }

        log.info("Found {} debt(s) with due reminders", debts.size());

        final AtomicInteger success = new AtomicInteger();
        final AtomicInteger failure = new AtomicInteger();

        final List<CompletableFuture<Void>> futures = debts.stream()
                .map(debt -> CompletableFuture.runAsync(
                        () -> processReminderDebt(debt, now, success, failure), taskExecutor))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("Reminder batch complete — success: {}, failed: {}", success.get(), failure.get());
    }

    private void processReminderDebt(final DebtDocument debt, final LocalDateTime now,
                                     final AtomicInteger success, final AtomicInteger failure) {
        final LocalDateTime reminderDate = debt.getNextReminderDate();
        advanceNextReminderDate(debt, now);

        try {
            debtRS.update(debt);
        } catch (Exception e) {
            log.error("DB update failed for debt {}, skipping reminder event — will retry next run: {}",
                    debt.getId(), e.getMessage(), e);
            failure.incrementAndGet();
            return;
        }

        try {
            produceReminderEvent(debt, reminderDate);
            success.incrementAndGet();
        } catch (Exception e) {
            log.error("Reminder event failed after DB update for debt {}: {}", debt.getId(), e.getMessage(), e);
            failure.incrementAndGet();
        }
    }

    private void produceReminderEvent(final DebtDocument debt, final LocalDateTime reminderDate) {
        debtEventProducer.produce(
                RabbitMQConstants.NOTIFICATION_EXCHANGE,
                RabbitMQConstants.DEBT_REMINDER_ROUTING_KEY,
                DebtReminderEvent.builder()
                        .debtId(debt.getId())
                        .ownerId(debt.getOwnerId())
                        .debtName(debt.getName())
                        .amount(debt.getAmount())
                        .nextReminderDate(reminderDate)
                        .channels(debt.getReminder() != null ? debt.getReminder().getChannels() : null)
                        .build()
        );
    }

    private void advanceNextReminderDate(final DebtDocument debt, final LocalDateTime now) {
        final int originalDay = debt.getFirstPaymentDate().getDayOfMonth();
        LocalDateTime next = PaymentPeriodUtil.advanceDate(
                debt.getNextReminderDate(), debt.getPaymentPeriodType(), originalDay);
        if (next == null) {
            debt.setNextReminderDate(null);
            log.info("No next reminder for ONE_TIME debt {}", debt.getId());
            return;
        }
        while (!next.isAfter(now)) {
            next = PaymentPeriodUtil.advanceDate(next, debt.getPaymentPeriodType(), originalDay);
            if (next == null) break;
        }
        if (next != null) debt.setNextReminderDate(next);
        log.info("Advanced next reminder for debt {} to {}", debt.getId(), next);
    }

    private void processAutoPaymentBatch(final List<DebtDocument> debts, final LocalDateTime now) {
        if (debts.isEmpty()) {
            return;
        }

        log.info("Found {} debt(s) with due auto-payments", debts.size());

        final AtomicInteger success = new AtomicInteger();
        final AtomicInteger failure = new AtomicInteger();

        final List<CompletableFuture<Void>> futures = debts.stream()
                .map(debt -> CompletableFuture.runAsync(
                        () -> processAutoPaymentDebt(debt, now, success, failure), taskExecutor))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("Auto-payment batch complete — success: {}, failed: {}", success.get(), failure.get());
    }

    private void processAutoPaymentDebt(final DebtDocument debt, final LocalDateTime now,
                                        final AtomicInteger success, final AtomicInteger failure) {
        recordAutoPayment(debt, now);

        if (debt.getInstallmentCount() != null
                && debt.getPayments().size() >= debt.getInstallmentCount()) {
            debt.setStatus(DebtStatus.COMPLETED);
            log.info("Debt {} marked as COMPLETED after auto-payment — all {} installments paid",
                    debt.getId(), debt.getInstallmentCount());
        } else {
            advanceNextAutoPaymentDate(debt);
        }

        try {
            debtRS.update(debt);
            success.incrementAndGet();
        } catch (Exception e) {
            log.error("Auto-payment recorded but DB update failed for debt {}: {}", debt.getId(), e.getMessage(), e);
            failure.incrementAndGet();
        }
    }

    private void processOverdueBatch(final List<DebtDocument> debts, final LocalDateTime now) {
        if (debts.isEmpty()) {
            return;
        }

        final AtomicInteger marked = new AtomicInteger();
        final List<CompletableFuture<Void>> futures = debts.stream()
                .map(debt -> CompletableFuture.runAsync(() -> {
                    if (isCurrentPeriodUnpaid(debt, now)) {
                        debt.setPaymentStatus(PaymentStatus.OVERDUE);
                        try {
                            debtRS.update(debt);
                            log.info("Debt {} payment status marked as OVERDUE", debt.getId());
                            marked.incrementAndGet();
                        } catch (Exception e) {
                            log.error("Failed to mark debt {} as OVERDUE: {}", debt.getId(), e.getMessage(), e);
                        }
                    }
                }, taskExecutor))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("Overdue batch complete — {} debt(s) marked as OVERDUE", marked.get());
    }

    private boolean isCurrentPeriodUnpaid(final DebtDocument debt, final LocalDateTime now) {
        if (debt.getPayments() == null || debt.getPayments().isEmpty()) {
            return true;
        }
        final LocalDateTime periodStart = PaymentPeriodUtil.getPeriodStart(
                debt.getFirstPaymentDate(), debt.getPaymentPeriodType(), now);
        return debt.getPayments().stream()
                .noneMatch(p -> p.isPaid() && p.getPaidTime() != null
                        && !p.getPaidTime().isBefore(periodStart));
    }

    private void recordAutoPayment(final DebtDocument debt, final LocalDateTime now) {
        final LocalDateTime periodStart = debt.getNextAutoPaymentDate();
        final int originalDay = debt.getFirstPaymentDate().getDayOfMonth();
        final LocalDateTime periodEnd = PaymentPeriodUtil.getPeriodEnd(
                periodStart, debt.getPaymentPeriodType(), originalDay);
        debt.addPayment(now, periodStart, periodEnd, debt.getAmount());
        debt.setPaymentStatus(PaymentStatus.PAID);
        log.info("Auto-payment recorded for debt: {}", debt.getId());
    }

    private void advanceNextAutoPaymentDate(final DebtDocument debt) {
        final int originalDay = debt.getFirstPaymentDate().getDayOfMonth();
        final LocalDateTime next = PaymentPeriodUtil.advanceDate(
                debt.getNextAutoPaymentDate(), debt.getPaymentPeriodType(), originalDay);
        if (next == null) {
            debt.setNextAutoPaymentDate(null);
            log.info("No next auto-payment for ONE_TIME debt {}", debt.getId());
            return;
        }
        debt.setNextAutoPaymentDate(next);
        log.info("Advanced next auto-payment for debt {} to {}", debt.getId(), next);
    }
}