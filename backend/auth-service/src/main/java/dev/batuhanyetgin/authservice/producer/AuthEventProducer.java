package dev.batuhanyetgin.authservice.producer;

import dev.batuhanyetgin.commonmodule.event.AbstractEventProducer;
import dev.batuhanyetgin.commonmodule.event.BaseEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuthEventProducer extends AbstractEventProducer<BaseEvent> {

    public AuthEventProducer(final RabbitTemplate rabbitTemplate) {
        super(rabbitTemplate);
    }
}