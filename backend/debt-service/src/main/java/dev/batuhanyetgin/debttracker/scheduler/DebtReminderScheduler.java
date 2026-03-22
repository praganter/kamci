package dev.batuhanyetgin.debttracker.scheduler;

import dev.batuhanyetgin.commonmodule.event.DebtReminderEvent;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.document.embedded.PaymentEmbedded;
import dev.batuhanyetgin.debttracker.enums.DebtStatus;
import dev.batuhanyetgin.debttracker.enums.PaymentPeriodType;
import dev.batuhanyetgin.debttracker.producer.DebtEventProducer;
import dev.batuhanyetgin.debttracker.repositoryservice.DebtRepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebtReminderScheduler {

    private final DebtRepositoryService debtRS;
    private final DebtEventProducer debtEventProducer;
    @Qualifier("taskExecutor")
    private final Executor taskExecutor;

    @Scheduled(cron = "0 0/15 * * * *")
    @SchedulerLock(name = "debtReminderScheduler", lockAtMostFor = "PT13M", lockAtLeastFor = "PT10M")
    public void processReminders() {
        final LocalDateTime now = LocalDateTime.now();
        log.info("Processing debt reminders/auto-payments at: {}", now);

        processReminderBatch(debtRS.findDueReminders(now), now);
        processAutoPaymentBatch(debtRS.findDueAutoPayments(now), now);
    }

    // ─── Reminder flow ────────────────────────────────────────────────────────

    private void processReminderBatch(final List<DebtDocument> debts, final LocalDateTime now) {
        if (debts.isEmpty()) return;

        log.info("Found {} debt(s) with due reminders", debts.size());

        final AtomicInteger success = new AtomicInteger();
        final AtomicInteger failure = new AtomicInteger();

        final List<CompletableFuture<Void>> futures = debts.stream()
                .map(debt -> CompletableFuture.runAsync(
                        () -> processReminderDebt(debt, success, failure), taskExecutor))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("Reminder batch complete — success: {}, failed: {}", success.get(), failure.get());
    }

    private void processReminderDebt(final DebtDocument debt,
                                     final AtomicInteger success, final AtomicInteger failure) {
        try {
            produceReminderEvent(debt);
        } catch (Exception e) {
            log.error("Failed to publish reminder for debt {} — will retry next run: {}",
                    debt.getId(), e.getMessage(), e);
            failure.incrementAndGet();
            return;
        }

        advanceNextReminderDate(debt);

        try {
            debtRS.update(debt);
            success.incrementAndGet();
        } catch (Exception e) {
            log.error("Reminder sent but DB update failed for debt {}: {}", debt.getId(), e.getMessage(), e);
            failure.incrementAndGet();
        }
    }

    private void produceReminderEvent(final DebtDocument debt) {
        debtEventProducer.produce(
                RabbitMQConstants.NOTIFICATION_EXCHANGE,
                RabbitMQConstants.DEBT_REMINDER_ROUTING_KEY,
                DebtReminderEvent.builder()
                        .debtId(debt.getId())
                        .ownerId(debt.getOwnerId())
                        .debtName(debt.getName())
                        .amount(debt.getAmount())
                        .nextReminderDate(debt.getNextReminderDate())
                        .channels(debt.getReminder().getChannels())
                        .build()
        );
    }

    private void advanceNextReminderDate(final DebtDocument debt) {
        final int originalDay = debt.getFirstPaymentDate().getDayOfMonth();
        final LocalDateTime next = advanceDate(debt.getNextReminderDate(), debt.getPaymentPeriodType(), originalDay);
        debt.setNextReminderDate(next);
        log.info("Advanced next reminder for debt {} to {}", debt.getId(), next);
    }

    // ─── Auto-payment flow ────────────────────────────────────────────────────

    private void processAutoPaymentBatch(final List<DebtDocument> debts, final LocalDateTime now) {
        if (debts.isEmpty()) return;

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

        if (debt.getLastPaymentDate() != null && !now.isBefore(debt.getLastPaymentDate())) {
            debt.setStatus(DebtStatus.PASSIVE);
            log.info("Debt {} marked as PASSIVE after auto-payment — lastPaymentDate reached", debt.getId());
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

    private void recordAutoPayment(final DebtDocument debt, final LocalDateTime now) {
        final List<PaymentEmbedded> payments = new ArrayList<>(debt.getPayments());
        payments.add(PaymentEmbedded.builder()
                .paid(true)
                .paidTime(now)
                .build());
        debt.setPayments(payments);
        log.info("Auto-payment recorded for debt: {}", debt.getId());
    }

    private void advanceNextAutoPaymentDate(final DebtDocument debt) {
        final int originalDay = debt.getFirstPaymentDate().getDayOfMonth();
        final LocalDateTime next = advanceDate(debt.getNextAutoPaymentDate(), debt.getPaymentPeriodType(), originalDay);
        debt.setNextAutoPaymentDate(next);
        log.info("Advanced next auto-payment for debt {} to {}", debt.getId(), next);
    }

    // ─── Shared date utilities ────────────────────────────────────────────────

    private LocalDateTime advanceDate(final LocalDateTime current, final PaymentPeriodType type,
                                      final int originalDay) {
        return switch (type) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> clampToDay(current.plusMonths(1), originalDay);
            case HALF_YEARLY -> clampToDay(current.plusMonths(6), originalDay);
            case YEARLY -> clampToDay(current.plusYears(1), originalDay);
        };
    }

    private LocalDateTime clampToDay(final LocalDateTime dt, final int targetDay) {
        final int maxDay = dt.toLocalDate().lengthOfMonth();
        return dt.withDayOfMonth(Math.min(targetDay, maxDay));
    }
}
