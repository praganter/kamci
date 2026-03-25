package dev.batuhanyetgin.notificationservice.consumer.dlq;

import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.commonmodule.event.UserRegisteredEvent;
import dev.batuhanyetgin.notificationservice.consumer.UserRegisteredConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredDlqConsumer {

    private final UserRegisteredConsumer userRegisteredConsumer;

    @RabbitListener(queues = RabbitMQConstants.USER_REGISTERED_DLQ)
    public void consume(final UserRegisteredEvent event, @Headers final Map<String, Object> headers) {
        log.error("DLQ: UserRegisteredEvent processing failed. userId={}, reason={}, queue={}",
                event.getUserId(),
                headers.get("x-first-death-reason"),
                headers.get("x-first-death-queue"));
        try {
            userRegisteredConsumer.consume(event);
            log.info("DLQ retry succeeded for userId: {}", event.getUserId());
        } catch (Exception e) {
            log.error("DLQ retry failed for userId: {}. Discarding. Error: {}",
                    event.getUserId(), e.getMessage(), e);
        }
    }
}
