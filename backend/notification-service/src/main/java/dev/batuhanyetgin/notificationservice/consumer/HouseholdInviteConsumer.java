package dev.batuhanyetgin.notificationservice.consumer;

import dev.batuhanyetgin.commonmodule.constant.CommonConstants;
import dev.batuhanyetgin.commonmodule.event.EventConsumer;
import dev.batuhanyetgin.commonmodule.event.HouseholdInviteEvent;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
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

import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HouseholdInviteConsumer implements EventConsumer<HouseholdInviteEvent> {

    private final DeviceTokenRepository deviceTokenRepository;
    private final FcmService fcmService;
    private final MailService mailService;
    private final MessageSource messageSource;
    private final NotificationService notificationService;

    @Override
    @RabbitListener(queues = RabbitMQConstants.HOUSEHOLD_INVITE_QUEUE)
    public void consume(final HouseholdInviteEvent event) {
        log.info("HouseholdInviteEvent received for householdId: {}, inviteeUserId: {}",
                event.getHouseholdId(), event.getInviteeUserId());

        deviceTokenRepository.findByUserId(event.getInviteeUserId()).ifPresentOrElse(
                tokenDoc -> {
                    final Locale locale = Locale.forLanguageTag(
                            StringUtils.hasText(tokenDoc.getLocale()) ? tokenDoc.getLocale() : CommonConstants.DEFAULT_LOCALE
                    );
                    final String title = messageSource.getMessage(
                            "push.household_invite.title", null, locale);
                    final String body = messageSource.getMessage(
                            "push.household_invite.body",
                            new Object[]{event.getInviterEmail()},
                            locale);
                    notificationService.persist(event.getInviteeUserId(), NotificationType.HOUSEHOLD_INVITE,
                            title, body, event.getHouseholdId());

                    if (StringUtils.hasText(tokenDoc.getFcmToken())) {
                        fcmService.send(tokenDoc.getFcmToken(), title, body,
                                Map.of("householdId", event.getHouseholdId()));
                        log.info("FCM household invite notification sent to userId: {}", event.getInviteeUserId());
                    }

                    final String destinationEmail = StringUtils.hasText(tokenDoc.getEmail())
                            ? tokenDoc.getEmail() : event.getInviteeEmail();

                    if (StringUtils.hasText(destinationEmail)) {
                        final String subject = messageSource.getMessage(
                                "mail.household_invite.subject",
                                new Object[]{event.getInviterEmail()},
                                locale);
                        final String emailBody = messageSource.getMessage(
                                "mail.household_invite.body",
                                new Object[]{event.getInviterEmail()},
                                locale);
                        try {
                            mailService.send(destinationEmail, subject, emailBody);
                            log.info("Email household invite notification sent to: {}", destinationEmail);
                        } catch (Exception e) {
                            log.error("Failed to send household invite email to: {}: {}",
                                    destinationEmail, e.getMessage());
                        }
                    }
                },
                () -> {
                    log.warn("No device token record found for inviteeUserId: {}", event.getInviteeUserId());
                    final Locale locale = Locale.forLanguageTag(CommonConstants.DEFAULT_LOCALE);
                    final String pushTitle = messageSource.getMessage(
                            "push.household_invite.title", null, locale);
                    final String pushBody = messageSource.getMessage(
                            "push.household_invite.body",
                            new Object[]{event.getInviterEmail()},
                            locale);
                    notificationService.persist(event.getInviteeUserId(), NotificationType.HOUSEHOLD_INVITE,
                            pushTitle, pushBody, event.getHouseholdId());

                    if (StringUtils.hasText(event.getInviteeEmail())) {
                        final String subject = messageSource.getMessage(
                                "mail.household_invite.subject",
                                new Object[]{event.getInviterEmail()},
                                locale);
                        final String emailBody = messageSource.getMessage(
                                "mail.household_invite.body",
                                new Object[]{event.getInviterEmail()},
                                locale);
                        try {
                            mailService.send(event.getInviteeEmail(), subject, emailBody);
                            log.info("Email household invite fallback notification sent to: {}", event.getInviteeEmail());
                        } catch (Exception e) {
                            log.error("Failed to send household invite fallback email to: {}: {}",
                                    event.getInviteeEmail(), e.getMessage());
                        }
                    }
                }
        );
    }
}
