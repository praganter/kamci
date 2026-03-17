package dev.batuhanyetgin.debttracker.scheduler;

import dev.batuhanyetgin.commonmodule.event.DebtReminderEvent;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.enums.PaymentPeriodType;
import dev.batuhanyetgin.debttracker.producer.DebtEventProducer;
import dev.batuhanyetgin.debttracker.repositoryservice.DebtRepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebtReminderScheduler {

    private final DebtRepositoryService debtRS;
    private final DebtEventProducer debtEventProducer;

    @Scheduled(cron = "0 0 * * * *")
    public void processReminders() {
        final LocalDateTime now = LocalDateTime.now();
        log.info("Processing debt reminders at: {}", now);

        final List<DebtDocument> dueDebts = debtRS.findDueReminders(now);
        log.info("Found {} debt(s) with due reminders", dueDebts.size());

        dueDebts.forEach(debt -> {
            produceReminderEvent(debt);
            advanceNextReminderDate(debt);
            debtRS.update(debt);
        });
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