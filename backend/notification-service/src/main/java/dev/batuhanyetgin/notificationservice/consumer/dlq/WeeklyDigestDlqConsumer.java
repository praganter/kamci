package dev.batuhanyetgin.notificationservice.consumer.dlq;

import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.commonmodule.event.WeeklyDigestEvent;
import dev.batuhanyetgin.notificationservice.consumer.WeeklyDigestConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyDigestDlqConsumer {

    private final WeeklyDigestConsumer weeklyDigestConsumer;

    @RabbitListener(queues = RabbitMQConstants.WEEKLY_DIGEST_DLQ)
    public void consume(final WeeklyDigestEvent event, @Headers final Map<String, Object> headers) {
        log.error("DLQ: WeeklyDigestEvent processing failed. ownerId={}, reason={}, queue={}",
                event.getOwnerId(),
                headers.get("x-first-death-reason"),
                headers.get("x-first-death-queue"));
        try {
            weeklyDigestConsumer.consume(event);
            log.info("DLQ retry succeeded for ownerId: {}", event.getOwnerId());
        } catch (Exception e) {
            log.error("DLQ retry failed for ownerId: {}. Discarding. Error: {}",
                    event.getOwnerId(), e.getMessage(), e);
        }
    }
}
