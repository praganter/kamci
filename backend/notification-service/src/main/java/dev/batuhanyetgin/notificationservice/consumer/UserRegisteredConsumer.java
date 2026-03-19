package dev.batuhanyetgin.notificationservice.consumer;

import dev.batuhanyetgin.commonmodule.event.EventConsumer;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.commonmodule.event.UserRegisteredEvent;
import dev.batuhanyetgin.notificationservice.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredConsumer implements EventConsumer<UserRegisteredEvent> {

    private final MailService mailService;
    private final MessageSource messageSource;

    @Override
    @RabbitListener(queues = RabbitMQConstants.USER_REGISTERED_QUEUE)
    public void consume(final UserRegisteredEvent event) {
        log.info("UserRegisteredEvent received for userId: {}", event.getUserId());

        final Locale locale = Locale.forLanguageTag(event.getLocale());
        final String subject = messageSource.getMessage("mail.welcome.subject", null, locale);
        final String body = messageSource.getMessage("mail.welcome.body", null, locale);

        mailService.send(event.getEmail(), subject, body);

        log.info("Welcome mail sent to: {}", event.getEmail());
    }
}
