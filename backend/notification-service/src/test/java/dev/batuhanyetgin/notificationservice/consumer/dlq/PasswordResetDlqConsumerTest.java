package dev.batuhanyetgin.notificationservice.consumer.dlq;

import dev.batuhanyetgin.commonmodule.event.PasswordResetEvent;
import dev.batuhanyetgin.notificationservice.consumer.PasswordResetConsumer;
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
class PasswordResetDlqConsumerTest {

    @Mock private PasswordResetConsumer passwordResetConsumer;

    @InjectMocks
    private PasswordResetDlqConsumer dlqConsumer;

    private PasswordResetEvent buildEvent() {
        return PasswordResetEvent.builder()
                .userId("user123")
                .email("user@example.com")
                .resetToken("reset-token-123")
                .build();
    }

    private Map<String, Object> buildHeaders() {
        return Map.of(
                "x-first-death-reason", "rejected",
                "x-first-death-queue", "password-reset-queue"
        );
    }

    @Nested
    @DisplayName("consume")
    class Consume {

        @Test
        @DisplayName("retry succeeds → delegates to main consumer")
        void retrySucceeds_delegatesToMainConsumer() {
            final PasswordResetEvent event = buildEvent();

            dlqConsumer.consume(event, buildHeaders());

            verify(passwordResetConsumer).consume(event);
        }

        @Test
        @DisplayName("retry fails → exception caught, not propagated")
        void retryFails_exceptionCaughtNotPropagated() {
            final PasswordResetEvent event = buildEvent();
            doThrow(new RuntimeException("SMTP unavailable"))
                    .when(passwordResetConsumer).consume(event);

            assertDoesNotThrow(() -> dlqConsumer.consume(event, buildHeaders()));
        }

        @Test
        @DisplayName("empty headers → no NullPointerException")
        void emptyHeaders_noException() {
            assertDoesNotThrow(() -> dlqConsumer.consume(buildEvent(), Map.of()));
        }
    }
}
