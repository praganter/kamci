package dev.batuhanyetgin.notificationservice.consumer;

import dev.batuhanyetgin.commonmodule.event.DebtAddedEvent;
import dev.batuhanyetgin.commonmodule.event.EventConsumer;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.notificationservice.document.DeviceTokenDocument;
import dev.batuhanyetgin.notificationservice.repository.DeviceTokenRepository;
import dev.batuhanyetgin.notificationservice.service.FcmService;
import dev.batuhanyetgin.notificationservice.service.MailService;
import org.springframework.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebtAddedConsumer implements EventConsumer<DebtAddedEvent> {

    private final DeviceTokenRepository deviceTokenRepository;
    private final FcmService fcmService;
    private final MailService mailService;
    private final MessageSource messageSource;

    @Override
    @RabbitListener(queues = RabbitMQConstants.DEBT_ADDED_QUEUE)
    public void consume(final DebtAddedEvent event) {
        log.info("DebtAddedEvent received for debtId: {}", event.getDebtId());

        if (event.getMemberUserIds() == null || event.getMemberUserIds().isEmpty()) {
            log.info("No members for debtId: {}, skipping", event.getDebtId());
            return;
        }

        final List<DeviceTokenDocument> tokenDocs = deviceTokenRepository
                .findByUserIdIn(event.getMemberUserIds());

        if (tokenDocs.isEmpty()) {
            log.warn("No device tokens found for members of debtId: {}", event.getDebtId());
            return;
        }

        final Locale locale = Locale.forLanguageTag(event.getLocale());

        final List<String> fcmTokens = tokenDocs.stream()
                .map(DeviceTokenDocument::getFcmToken)
                .filter(StringUtils::hasText)
                .toList();

        if (!fcmTokens.isEmpty()) {
            final String title = messageSource.getMessage("push.debt_added.title", null, locale);
            final String body = messageSource.getMessage(
                    "push.debt_added.body",
                    new Object[]{event.getDebtName(), event.getAmount()},
                    locale
            );
            fcmService.sendMulticast(fcmTokens, title, body, Map.of("debtId", event.getDebtId()));
            log.info("FCM multicast sent for debtId: {} to {} members", event.getDebtId(), fcmTokens.size());
        }

        tokenDocs.stream()
                .filter(doc -> StringUtils.hasText(doc.getEmail()))
                .forEach(doc -> {
                    final Locale memberLocale = Locale.forLanguageTag(
                            StringUtils.hasText(doc.getLocale()) ? doc.getLocale() : "tr");
                    final String subject = messageSource.getMessage(
                            "mail.debt_added.subject", new Object[]{event.getDebtName()}, memberLocale);
                    final String emailBody = messageSource.getMessage(
                            "mail.debt_added.body", new Object[]{event.getDebtName(), event.getAmount()}, memberLocale);
                    try {
                        mailService.send(doc.getEmail(), subject, emailBody);
                        log.info("Debt-added email sent to: {} for debtId: {}", doc.getEmail(), event.getDebtId());
                    } catch (Exception e) {
                        log.error("Failed to send debt-added email to: {} for debtId: {}: {}",
                                doc.getEmail(), event.getDebtId(), e.getMessage());
                    }
                });
    }
}
