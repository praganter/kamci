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
import ChronoUnit;
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
        processOverdueBatch(debtRS.findActiveManualDueDebts(now), now);
    }

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
        final LocalDateTime reminderDate = debt.getNextReminderDate();
        advanceNextReminderDate(debt);

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

    private void advanceNextReminderDate(final DebtDocument debt) {
        final int originalDay = debt.getFirstPaymentDate().getDayOfMonth();
        final LocalDateTime next = advanceDate(debt.getNextReminderDate(), debt.getPaymentPeriodType(), originalDay);
        debt.setNextReminderDate(next);
        log.info("Advanced next reminder for debt {} to {}", debt.getId(), next);
    }

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

        if (debt.getInstallmentCount() != null
                && debt.getPayments().size() >= debt.getInstallmentCount()) {
            debt.setStatus(DebtStatus.PASSIVE);
            log.info("Debt {} marked as PASSIVE after auto-payment — all {} installments paid",
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
        if (debts.isEmpty()) return;

        final AtomicInteger marked = new AtomicInteger();
        final List<CompletableFuture<Void>> futures = debts.stream()
                .map(debt -> CompletableFuture.runAsync(() -> {
                    if (isCurrentPeriodUnpaid(debt, now)) {
                        debt.setStatus(DebtStatus.OVERDUE);
                        try {
                            debtRS.update(debt);
                            log.info("Debt {} marked as OVERDUE", debt.getId());
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
        final LocalDateTime periodStart = switch (debt.getPaymentPeriodType()) {
            case DAILY -> {
                final long days = ChronoUnit.DAYS.between(debt.getFirstPaymentDate(), now);
                yield debt.getFirstPaymentDate().plusDays(days);
            }
            case WEEKLY -> {
                final long weeks = ChronoUnit.WEEKS.between(debt.getFirstPaymentDate(), now);
                yield debt.getFirstPaymentDate().plusWeeks(weeks);
            }
            case BI_WEEKLY -> {
                final long biWeeks = ChronoUnit.WEEKS.between(debt.getFirstPaymentDate(), now) / 2;
                yield debt.getFirstPaymentDate().plusWeeks(biWeeks * 2);
            }
            case MONTHLY -> {
                final long months = ChronoUnit.MONTHS.between(debt.getFirstPaymentDate(), now);
                yield debt.getFirstPaymentDate().plusMonths(months);
            }
            case QUARTERLY -> {
                final long quarters = ChronoUnit.MONTHS.between(debt.getFirstPaymentDate(), now) / 3;
                yield debt.getFirstPaymentDate().plusMonths(quarters * 3);
            }
            case HALF_YEARLY -> {
                final long halfYears = ChronoUnit.MONTHS.between(debt.getFirstPaymentDate(), now) / 6;
                yield debt.getFirstPaymentDate().plusMonths(halfYears * 6);
            }
            case YEARLY -> {
                final long years = ChronoUnit.YEARS.between(debt.getFirstPaymentDate(), now);
                yield debt.getFirstPaymentDate().plusYears(years);
            }
        };
        return debt.getPayments().stream()
                .noneMatch(p -> p.isPaid() && p.getPaidTime() != null
                        && !p.getPaidTime().isBefore(periodStart));
    }

    private void recordAutoPayment(final DebtDocument debt, final LocalDateTime now) {
        debt.addPayment(now);
        log.info("Auto-payment recorded for debt: {}", debt.getId());
    }

    private void advanceNextAutoPaymentDate(final DebtDocument debt) {
        final int originalDay = debt.getFirstPaymentDate().getDayOfMonth();
        final LocalDateTime next = advanceDate(debt.getNextAutoPaymentDate(), debt.getPaymentPeriodType(), originalDay);
        debt.setNextAutoPaymentDate(next);
        log.info("Advanced next auto-payment for debt {} to {}", debt.getId(), next);
    }

    private LocalDateTime advanceDate(final LocalDateTime current, final PaymentPeriodType type,
                                      final int originalDay) {
        return switch (type) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case BI_WEEKLY -> current.plusWeeks(2);
            case MONTHLY -> clampToDay(current.plusMonths(1), originalDay);
            case QUARTERLY -> clampToDay(current.plusMonths(3), originalDay);
            case HALF_YEARLY -> clampToDay(current.plusMonths(6), originalDay);
            case YEARLY -> clampToDay(current.plusYears(1), originalDay);
        };
    }

    private LocalDateTime clampToDay(final LocalDateTime dt, final int targetDay) {
        final int maxDay = dt.toLocalDate().lengthOfMonth();
        return dt.withDayOfMonth(Math.min(targetDay, maxDay));
    }
}
