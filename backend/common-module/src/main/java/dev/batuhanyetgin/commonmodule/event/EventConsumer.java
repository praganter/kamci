package dev.batuhanyetgin.commonmodule.event;

public interface EventConsumer<T extends BaseEvent> {
    void consume(final T event);
}