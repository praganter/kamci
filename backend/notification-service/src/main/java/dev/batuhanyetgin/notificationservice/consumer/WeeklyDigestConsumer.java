package dev.batuhanyetgin.notificationservice.consumer;

import dev.batuhanyetgin.commonmodule.constant.CommonConstants;
import dev.batuhanyetgin.commonmodule.event.EventConsumer;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.commonmodule.event.WeeklyDigestEvent;
import dev.batuhanyetgin.commonmodule.event.WeeklyDigestItem;
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
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyDigestConsumer implements EventConsumer<WeeklyDigestEvent> {

    private final DeviceTokenRepository deviceTokenRepository;
    private final FcmService fcmService;
    private final MailService mailService;
    private final MessageSource messageSource;
    private final NotificationService notificationService;

    @Override
    @RabbitListener(queues = RabbitMQConstants.WEEKLY_DIGEST_QUEUE)
    public void consume(final WeeklyDigestEvent event) {
        log.info("WeeklyDigestEvent received for ownerId: {}, items: {}",
                event.getOwnerId(), event.getItems() == null ? 0 : event.getItems().size());

        final List<WeeklyDigestItem> items = event.getItems();
        if (items == null || items.isEmpty()) {
            log.info("No items in weekly digest for ownerId: {}, skipping", event.getOwnerId());
            return;
        }

        final DeviceTokenDocument tokenDoc = deviceTokenRepository.findByUserId(event.getOwnerId())
                .orElse(null);

        final Locale locale = Locale.forLanguageTag(
                tokenDoc != null && StringUtils.hasText(tokenDoc.getLocale())
                        ? tokenDoc.getLocale() : CommonConstants.DEFAULT_LOCALE
        );

        final String itemLines = items.stream()
                .map(item -> String.format("- %s: %s (%s)",
                        item.getDebtName(), item.getAmount(), DateTimeUtil.format(item.getDueDate())))
                .collect(Collectors.joining("\n"));

        final String title = messageSource.getMessage(
                "push.weekly_digest.title", null, locale);
        final String body = messageSource.getMessage(
                "push.weekly_digest.body", new Object[]{items.size(), event.getTotalAmount()}, locale);

        notificationService.persist(event.getOwnerId(), NotificationType.WEEKLY_DIGEST, title, body, null);

        if (tokenDoc == null) {
            log.warn("No device token record found for ownerId: {}", event.getOwnerId());
            return;
        }

        if (StringUtils.hasText(tokenDoc.getFcmToken())) {
            fcmService.send(tokenDoc.getFcmToken(), title, body, Map.of("type", "weekly_digest"));
            log.info("FCM weekly digest sent for ownerId: {}", event.getOwnerId());
        }

        if (StringUtils.hasText(tokenDoc.getEmail())) {
            final String subject = messageSource.getMessage(
                    "mail.weekly_digest.subject", new Object[]{items.size()}, locale);
            final String emailBody = messageSource.getMessage(
                    "mail.weekly_digest.body", new Object[]{event.getTotalAmount(), itemLines}, locale);
            try {
                mailService.send(tokenDoc.getEmail(), subject, emailBody);
                log.info("Email weekly digest sent for ownerId: {} to: {}", event.getOwnerId(), tokenDoc.getEmail());
            } catch (Exception e) {
                log.error("Failed to send weekly digest email for ownerId: {}: {}", event.getOwnerId(), e.getMessage());
            }
        }
    }
}
