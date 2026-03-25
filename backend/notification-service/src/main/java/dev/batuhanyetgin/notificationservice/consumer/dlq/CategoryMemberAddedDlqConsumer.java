package dev.batuhanyetgin.notificationservice.consumer.dlq;

import dev.batuhanyetgin.commonmodule.event.CategoryMemberAddedEvent;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.notificationservice.consumer.CategoryMemberAddedConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryMemberAddedDlqConsumer {

    private final CategoryMemberAddedConsumer categoryMemberAddedConsumer;

    @RabbitListener(queues = RabbitMQConstants.CATEGORY_MEMBER_ADDED_DLQ)
    public void consume(final CategoryMemberAddedEvent event, @Headers final Map<String, Object> headers) {
        log.error("DLQ: CategoryMemberAddedEvent processing failed. categoryId={}, reason={}, queue={}",
                event.getCategoryId(),
                headers.get("x-first-death-reason"),
                headers.get("x-first-death-queue"));
        try {
            categoryMemberAddedConsumer.consume(event);
            log.info("DLQ retry succeeded for categoryId: {}", event.getCategoryId());
        } catch (Exception e) {
            log.error("DLQ retry failed for categoryId: {}. Discarding. Error: {}",
                    event.getCategoryId(), e.getMessage(), e);
        }
    }
}
