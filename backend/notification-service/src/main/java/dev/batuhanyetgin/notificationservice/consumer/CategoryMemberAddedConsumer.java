package dev.batuhanyetgin.notificationservice.consumer;

import dev.batuhanyetgin.commonmodule.constant.CommonConstants;
import dev.batuhanyetgin.commonmodule.event.CategoryMemberAddedEvent;
import dev.batuhanyetgin.commonmodule.event.EventConsumer;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.notificationservice.repository.DeviceTokenRepository;
import dev.batuhanyetgin.notificationservice.service.FcmService;
import dev.batuhanyetgin.notificationservice.service.MailService;
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
public class CategoryMemberAddedConsumer implements EventConsumer<CategoryMemberAddedEvent> {

    private final DeviceTokenRepository deviceTokenRepository;
    private final FcmService fcmService;
    private final MailService mailService;
    private final MessageSource messageSource;

    @Override
    @RabbitListener(queues = RabbitMQConstants.CATEGORY_MEMBER_ADDED_QUEUE)
    public void consume(final CategoryMemberAddedEvent event) {
        log.info("CategoryMemberAddedEvent received for categoryId: {}, memberUserId: {}",
                event.getCategoryId(), event.getMemberUserId());

        deviceTokenRepository.findByUserId(event.getMemberUserId()).ifPresentOrElse(
                tokenDoc -> {
                    final Locale locale = Locale.forLanguageTag(
                            StringUtils.hasText(tokenDoc.getLocale()) ? tokenDoc.getLocale() : CommonConstants.DEFAULT_LOCALE
                    );

                    if (StringUtils.hasText(tokenDoc.getFcmToken())) {
                        final String title = messageSource.getMessage(
                                "push.category_member_added.title", null, locale);
                        final String body = messageSource.getMessage(
                                "push.category_member_added.body",
                                new Object[]{event.getCategoryName()},
                                locale);
                        fcmService.send(tokenDoc.getFcmToken(), title, body,
                                Map.of("categoryId", event.getCategoryId()));
                        log.info("FCM category member added notification sent to userId: {}", event.getMemberUserId());
                    }

                    if (StringUtils.hasText(tokenDoc.getEmail())) {
                        final String subject = messageSource.getMessage(
                                "mail.category_member_added.subject",
                                new Object[]{event.getCategoryName()},
                                locale);
                        final String emailBody = messageSource.getMessage(
                                "mail.category_member_added.body",
                                new Object[]{event.getCategoryName()},
                                locale);
                        try {
                            mailService.send(tokenDoc.getEmail(), subject, emailBody);
                            log.info("Email category member added notification sent to: {}", tokenDoc.getEmail());
                        } catch (Exception e) {
                            log.error("Failed to send category member added email to: {}: {}",
                                    tokenDoc.getEmail(), e.getMessage());
                        }
                    }
                },
                () -> log.warn("No device token record found for memberUserId: {}", event.getMemberUserId())
        );
    }
}