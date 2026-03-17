package dev.batuhanyetgin.commonmodule.event;

public interface EventConsumer<T extends BaseEvent> {
    void consume(T event);
}