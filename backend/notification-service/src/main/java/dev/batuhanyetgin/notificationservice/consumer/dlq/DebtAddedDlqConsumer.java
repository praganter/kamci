package dev.batuhanyetgin.notificationservice.consumer.dlq;

import dev.batuhanyetgin.commonmodule.event.DebtAddedEvent;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.notificationservice.consumer.DebtAddedConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebtAddedDlqConsumer {

    private final DebtAddedConsumer debtAddedConsumer;

    @RabbitListener(queues = RabbitMQConstants.DEBT_ADDED_DLQ)
    public void consume(final DebtAddedEvent event, @Headers final Map<String, Object> headers) {
        log.error("DLQ: DebtAddedEvent processing failed. debtId={}, reason={}, queue={}",
                event.getDebtId(),
                headers.get("x-first-death-reason"),
                headers.get("x-first-death-queue"));
        try {
            debtAddedConsumer.consume(event);
            log.info("DLQ retry succeeded for debtId: {}", event.getDebtId());
        } catch (Exception e) {
            log.error("DLQ retry failed for debtId: {}. Discarding. Error: {}",
                    event.getDebtId(), e.getMessage(), e);
        }
    }
}
