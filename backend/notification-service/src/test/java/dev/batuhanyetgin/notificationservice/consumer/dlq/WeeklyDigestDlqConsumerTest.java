package dev.batuhanyetgin.notificationservice.consumer.dlq;

import dev.batuhanyetgin.commonmodule.event.WeeklyDigestEvent;
import dev.batuhanyetgin.notificationservice.consumer.WeeklyDigestConsumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeeklyDigestDlqConsumerTest {

    @Mock private WeeklyDigestConsumer weeklyDigestConsumer;

    @InjectMocks
    private WeeklyDigestDlqConsumer dlqConsumer;

    private WeeklyDigestEvent buildEvent() {
        return WeeklyDigestEvent.builder()
                .ownerId("owner1")
                .totalAmount(BigDecimal.TEN)
                .build();
    }

    private Map<String, Object> buildHeaders() {
        return Map.of(
                "x-first-death-reason", "rejected",
                "x-first-death-queue", "weekly-digest-queue"
        );
    }

    @Nested
    @DisplayName("consume")
    class Consume {

        @Test
        @DisplayName("retry succeeds → delegates to main consumer")
        void retrySucceeds_delegatesToMainConsumer() {
            final WeeklyDigestEvent event = buildEvent();

            dlqConsumer.consume(event, buildHeaders());

            verify(weeklyDigestConsumer).consume(event);
        }

        @Test
        @DisplayName("retry fails → exception caught, not propagated")
        void retryFails_exceptionCaughtNotPropagated() {
            final WeeklyDigestEvent event = buildEvent();
            doThrow(new RuntimeException("Processing failed"))
                    .when(weeklyDigestConsumer).consume(event);

            assertDoesNotThrow(() -> dlqConsumer.consume(event, buildHeaders()));
        }

        @Test
        @DisplayName("empty headers → no NullPointerException")
        void emptyHeaders_noException() {
            assertDoesNotThrow(() -> dlqConsumer.consume(buildEvent(), Map.of()));
        }
    }
}
