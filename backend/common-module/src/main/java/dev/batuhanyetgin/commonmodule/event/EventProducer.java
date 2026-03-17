package dev.batuhanyetgin.commonmodule.event;

public interface EventProducer<T extends BaseEvent> {
    void produce(final String exchange, final String routingKey, final T event);
}