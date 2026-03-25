package dev.batuhanyetgin.notificationservice.consumer.dlq;

import dev.batuhanyetgin.commonmodule.event.PasswordResetEvent;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.notificationservice.consumer.PasswordResetConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetDlqConsumer {

    private final PasswordResetConsumer passwordResetConsumer;

    @RabbitListener(queues = RabbitMQConstants.PASSWORD_RESET_DLQ)
    public void consume(final PasswordResetEvent event, @Headers final Map<String, Object> headers) {
        log.error("DLQ: PasswordResetEvent processing failed. userId={}, reason={}, queue={}",
                event.getUserId(),
                headers.get("x-first-death-reason"),
                headers.get("x-first-death-queue"));
        try {
            passwordResetConsumer.consume(event);
            log.info("DLQ retry succeeded for userId: {}", event.getUserId());
        } catch (Exception e) {
            log.error("DLQ retry failed for userId: {}. Discarding. Error: {}",
                    event.getUserId(), e.getMessage(), e);
        }
    }
}
