package dev.batuhanyetgin.commonmodule.event;

public interface EventProducer<T extends BaseEvent> {
    void produce(String exchange, String routingKey, T event);
}