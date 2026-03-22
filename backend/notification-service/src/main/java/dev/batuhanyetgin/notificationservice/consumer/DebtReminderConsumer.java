package dev.batuhanyetgin.notificationservice.consumer;

import dev.batuhanyetgin.commonmodule.event.DebtReminderEvent;
import dev.batuhanyetgin.commonmodule.event.EventConsumer;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.commonmodule.event.ReminderChannelType;
import dev.batuhanyetgin.commonmodule.util.DateTimeUtil;
import dev.batuhanyetgin.notificationservice.repository.DeviceTokenRepository;
import dev.batuhanyetgin.notificationservice.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebtReminderConsumer implements EventConsumer<DebtReminderEvent> {

    private final DeviceTokenRepository deviceTokenRepository;
    private final FcmService fcmService;
    private final MessageSource messageSource;

    @Override
    @RabbitListener(queues = RabbitMQConstants.DEBT_REMINDER_QUEUE)
    public void consume(final DebtReminderEvent event) {
        log.info("DebtReminderEvent received for debtId: {}, ownerId: {}", event.getDebtId(), event.getOwnerId());

        if (event.getChannels() == null || !event.getChannels().contains(ReminderChannelType.IN_APP)) {
            log.info("IN_APP channel not set for debtId: {}, skipping", event.getDebtId());
            return;
        }

        deviceTokenRepository.findByUserId(event.getOwnerId()).ifPresentOrElse(
                tokenDoc -> {
                    final Locale locale = Locale.forLanguageTag(
                            StringUtils.hasText(tokenDoc.getLocale()) ? tokenDoc.getLocale() : "tr"
                    );
                    final String formattedDate = DateTimeUtil.format(event.getNextReminderDate());
                    final String title = messageSource.getMessage(
                            "push.reminder.title",
                            new Object[]{event.getDebtName()},
                            locale
                    );
                    final String body = messageSource.getMessage(
                            "push.reminder.body",
                            new Object[]{event.getDebtName(), formattedDate},
                            locale
                    );
                    fcmService.send(tokenDoc.getFcmToken(), title, body,
                            Map.of("debtId", event.getDebtId()));
                    log.info("FCM reminder sent for debtId: {}", event.getDebtId());
                },
                () -> log.warn("No device token found for ownerId: {}", event.getOwnerId())
        );
    }
}
