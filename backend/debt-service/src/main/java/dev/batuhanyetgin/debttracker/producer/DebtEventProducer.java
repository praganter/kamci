package dev.batuhanyetgin.debttracker.producer;

import dev.batuhanyetgin.commonmodule.event.AbstractEventProducer;
import dev.batuhanyetgin.commonmodule.event.BaseEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class DebtEventProducer extends AbstractEventProducer<BaseEvent> {

    public DebtEventProducer(final RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }
}