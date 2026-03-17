package dev.batuhanyetgin.commonmodule.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractEventProducer<T extends BaseEvent> implements EventProducer<T> {

    protected final RabbitTemplate rabbitTemplate;

    @Override
    public void produce(final String exchange, final String routingKey, final T event) {
        log.info("Sending event to Exchange: {}, RoutingKey: {}, Event: {}", exchange, routingKey, event);
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            log.info("Event successfully sent: {}", event);
        } catch (Exception e) {
            log.error("Error occurred while sending event to Exchange: {}, RoutingKey: {}. Exception: {}",
                    exchange, routingKey, e.getMessage(), e);
            throw e;
        }
    }
}