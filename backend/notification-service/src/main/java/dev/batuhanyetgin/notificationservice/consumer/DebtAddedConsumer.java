package dev.batuhanyetgin.notificationservice.consumer;

import dev.batuhanyetgin.commonmodule.event.DebtAddedEvent;
import dev.batuhanyetgin.commonmodule.event.EventConsumer;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.notificationservice.repository.DeviceTokenRepository;
import dev.batuhanyetgin.notificationservice.service.FcmService;
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
    private final MessageSource messageSource;

    @Override
    @RabbitListener(queues = RabbitMQConstants.DEBT_ADDED_QUEUE)
    public void consume(final DebtAddedEvent event) {
        log.info("DebtAddedEvent received for debtId: {}", event.getDebtId());

        if (event.getMemberUserIds() == null || event.getMemberUserIds().isEmpty()) {
            log.info("No members for debtId: {}, skipping", event.getDebtId());
            return;
        }

        final List<String> tokens = deviceTokenRepository
                .findByUserIdIn(event.getMemberUserIds())
                .stream()
                .map(doc -> doc.getFcmToken())
                .toList();

        if (tokens.isEmpty()) {
            log.warn("No device tokens found for members of debtId: {}", event.getDebtId());
            return;
        }

        final Locale locale = Locale.forLanguageTag(event.getLocale());
        final String title = messageSource.getMessage("push.debt_added.title", null, locale);
        final String body = messageSource.getMessage(
                "push.debt_added.body",
                new Object[]{event.getDebtName(), event.getAmount()},
                locale
        );

        fcmService.sendMulticast(tokens, title, body, Map.of("debtId", event.getDebtId()));
        log.info("FCM multicast sent for debtId: {} to {} members", event.getDebtId(), tokens.size());
    }
}
