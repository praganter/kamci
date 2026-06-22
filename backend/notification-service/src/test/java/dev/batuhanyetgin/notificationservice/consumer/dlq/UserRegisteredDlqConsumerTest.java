package dev.batuhanyetgin.notificationservice.consumer.dlq;

import dev.batuhanyetgin.commonmodule.event.UserRegisteredEvent;
import dev.batuhanyetgin.notificationservice.consumer.UserRegisteredConsumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegisteredDlqConsumerTest {

    @Mock private UserRegisteredConsumer userRegisteredConsumer;

    @InjectMocks
    private UserRegisteredDlqConsumer dlqConsumer;

    private UserRegisteredEvent buildEvent() {
        return UserRegisteredEvent.builder()
                .userId("user123")
                .email("user@example.com")
                .build();
    }

    private Map<String, Object> buildHeaders() {
        return Map.of(
                "x-first-death-reason", "rejected",
                "x-first-death-queue", "user-registered-queue"
        );
    }

    @Nested
    @DisplayName("consume")
    class Consume {

        @Test
        @DisplayName("retry succeeds → delegates to main consumer")
        void retrySucceeds_delegatesToMainConsumer() {
            final UserRegisteredEvent event = buildEvent();

            dlqConsumer.consume(event, buildHeaders());

            verify(userRegisteredConsumer).consume(event);
        }

        @Test
        @DisplayName("retry fails → exception caught, not propagated")
        void retryFails_exceptionCaughtNotPropagated() {
            final UserRegisteredEvent event = buildEvent();
            doThrow(new RuntimeException("SMTP unavailable"))
                    .when(userRegisteredConsumer).consume(event);

            assertDoesNotThrow(() -> dlqConsumer.consume(event, buildHeaders()));
        }

        @Test
        @DisplayName("empty headers → no NullPointerException")
        void emptyHeaders_noException() {
            assertDoesNotThrow(() -> dlqConsumer.consume(buildEvent(), Map.of()));
        }
    }
}
