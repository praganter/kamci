package dev.batuhanyetgin.notificationservice.consumer;

import dev.batuhanyetgin.commonmodule.event.EventConsumer;
import dev.batuhanyetgin.commonmodule.event.PasswordResetEvent;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.notificationservice.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetConsumer implements EventConsumer<PasswordResetEvent> {

    private final MailService mailService;
    private final MessageSource messageSource;

    @Value("${app.password-reset-deep-link}")
    private String passwordResetDeepLink;

    @Override
    @RabbitListener(queues = RabbitMQConstants.PASSWORD_RESET_QUEUE)
    public void consume(final PasswordResetEvent event) {
        log.info("PasswordResetEvent received for userId: {}", event.getUserId());

        final Locale locale = Locale.forLanguageTag(event.getLocale());
        final String subject = messageSource.getMessage("mail.password_reset.subject", null, locale);
        final String body = messageSource.getMessage("mail.password_reset.body",
                new Object[]{passwordResetDeepLink, event.getResetToken()}, locale);

        mailService.send(event.getEmail(), subject, body);

        log.info("Password reset mail sent to: {}", event.getEmail());
    }
}