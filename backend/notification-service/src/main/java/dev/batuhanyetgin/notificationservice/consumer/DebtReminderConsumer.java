package dev.batuhanyetgin.notificationservice.consumer;

import dev.batuhanyetgin.commonmodule.constant.CommonConstants;
import dev.batuhanyetgin.commonmodule.event.DebtReminderEvent;
import dev.batuhanyetgin.commonmodule.event.EventConsumer;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.commonmodule.event.ReminderChannelType;
import dev.batuhanyetgin.commonmodule.util.DateTimeUtil;
import dev.batuhanyetgin.notificationservice.document.DeviceTokenDocument;
import dev.batuhanyetgin.notificationservice.document.NotificationType;
import dev.batuhanyetgin.notificationservice.repository.DeviceTokenRepository;
import dev.batuhanyetgin.notificationservice.service.FcmService;
import dev.batuhanyetgin.notificationservice.service.MailService;
import dev.batuhanyetgin.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebtReminderConsumer implements EventConsumer<DebtReminderEvent> {

    private final DeviceTokenRepository deviceTokenRepository;
    private final FcmService fcmService;
    private final MailService mailService;
    private final MessageSource messageSource;
    private final NotificationService notificationService;

    @Override
    @RabbitListener(queues = RabbitMQConstants.DEBT_REMINDER_QUEUE)
    public void consume(final DebtReminderEvent event) {
        log.info("DebtReminderEvent received for debtId: {}, ownerId: {}", event.getDebtId(), event.getOwnerId());

        final List<ReminderChannelType> channels = event.getChannels();
        if (channels == null || channels.isEmpty()) {
            log.info("No channels set for debtId: {}, skipping", event.getDebtId());
            return;
        }

        final DeviceTokenDocument tokenDoc = deviceTokenRepository.findByUserId(event.getOwnerId())
                .orElse(null);

        final Locale locale = Locale.forLanguageTag(
                tokenDoc != null && StringUtils.hasText(tokenDoc.getLocale())
                        ? tokenDoc.getLocale() : CommonConstants.DEFAULT_LOCALE
        );
        final String formattedDate = DateTimeUtil.format(event.getNextReminderDate());
        final String title = messageSource.getMessage(
                "push.reminder.title", new Object[]{event.getDebtName()}, locale);
        final String body = messageSource.getMessage(
                "push.reminder.body", new Object[]{event.getDebtName(), formattedDate}, locale);
        notificationService.persist(event.getOwnerId(), NotificationType.DEBT_REMINDER, title, body, event.getDebtId());

        if (tokenDoc == null) {
            log.warn("No device token record found for ownerId: {}", event.getOwnerId());
            return;
        }

        if (channels.contains(ReminderChannelType.IN_APP) && StringUtils.hasText(tokenDoc.getFcmToken())) {
            fcmService.send(tokenDoc.getFcmToken(), title, body, Map.of("debtId", event.getDebtId()));
            log.info("FCM reminder sent for debtId: {}", event.getDebtId());
        }

        if (channels.contains(ReminderChannelType.EMAIL) && StringUtils.hasText(tokenDoc.getEmail())) {
            final String subject = messageSource.getMessage(
                    "mail.reminder.subject", new Object[]{event.getDebtName()}, locale);
            final String emailBody = messageSource.getMessage(
                    "mail.reminder.body", new Object[]{event.getDebtName(), event.getAmount(), formattedDate}, locale);
            try {
                mailService.send(tokenDoc.getEmail(), subject, emailBody);
                log.info("Email reminder sent for debtId: {} to: {}", event.getDebtId(), tokenDoc.getEmail());
            } catch (Exception e) {
                log.error("Failed to send reminder email for debtId: {}: {}", event.getDebtId(), e.getMessage());
            }
        }
    }
}
