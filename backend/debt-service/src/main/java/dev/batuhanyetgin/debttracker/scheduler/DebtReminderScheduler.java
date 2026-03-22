package dev.batuhanyetgin.debttracker.scheduler;

import dev.batuhanyetgin.commonmodule.event.DebtReminderEvent;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.debttracker.document.DebtDocument;
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

    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "debtReminderScheduler", lockAtMostFor = "PT55M", lockAtLeastFor = "PT55M")
    public void processReminders() {
        final LocalDateTime now = LocalDateTime.now();
        log.info("Processing debt reminders at: {}", now);

        final List<DebtDocument> dueDebts = debtRS.findDueReminders(now);
        log.info("Found {} debt(s) with due reminders", dueDebts.size());

        if (dueDebts.isEmpty()) return;

        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);

        final List<CompletableFuture<Void>> futures = dueDebts.stream()
                .map(debt -> CompletableFuture.runAsync(
                        () -> processDebt(debt, successCount, failureCount), taskExecutor))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("Reminder batch complete — success: {}, failed: {}", successCount.get(), failureCount.get());
        if (failureCount.get() > 0) {
            log.warn("{} reminder(s) failed and will be retried on the next run", failureCount.get());
        }
    }

    private void processDebt(final DebtDocument debt, final AtomicInteger successCount, final AtomicInteger failureCount) {
        try {
            produceReminderEvent(debt);
        } catch (Exception e) {
            log.error("Failed to publish reminder for debt {} — date not advanced, will retry next run: {}",
                    debt.getId(), e.getMessage(), e);
            failureCount.incrementAndGet();
            return;
        }

        advanceNextReminderDate(debt);

        try {
            debtRS.update(debt);
            successCount.incrementAndGet();
        } catch (Exception e) {
            log.error("Reminder sent but DB update failed for debt {} — next run may send duplicate reminder: {}",
                    debt.getId(), e.getMessage(), e);
            failureCount.incrementAndGet();
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
        final LocalDateTime current = debt.getNextReminderDate();
        final PaymentPeriodType periodType = debt.getPaymentPeriodType();

        final LocalDateTime next = switch (periodType) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
            case HALF_YEARLY -> current.plusMonths(6);
            case YEARLY -> current.plusYears(1);
        };

        debt.setNextReminderDate(next);
        log.info("Advanced next reminder for debt {} to {}", debt.getId(), next);
    }
}