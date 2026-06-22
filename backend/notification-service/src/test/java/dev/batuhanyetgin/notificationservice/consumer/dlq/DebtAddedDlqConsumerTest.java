package dev.batuhanyetgin.notificationservice.consumer.dlq;

import dev.batuhanyetgin.commonmodule.event.DebtAddedEvent;
import dev.batuhanyetgin.notificationservice.consumer.DebtAddedConsumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebtAddedDlqConsumerTest {

    @Mock private DebtAddedConsumer debtAddedConsumer;

    @InjectMocks
    private DebtAddedDlqConsumer dlqConsumer;

    private DebtAddedEvent buildEvent() {
        return DebtAddedEvent.builder()
                .debtId("debt123")
                .debtName("Test Debt")
                .amount(BigDecimal.TEN)
                .memberUserIds(List.of("member1"))
                .locale("en")
                .build();
    }

    private Map<String, Object> buildHeaders() {
        return Map.of(
                "x-first-death-reason", "rejected",
                "x-first-death-queue", "debt-added-queue"
        );
    }

    @Nested
    @DisplayName("consume")
    class Consume {

        @Test
        @DisplayName("retry succeeds → delegates to main consumer")
        void retrySucceeds_delegatesToMainConsumer() {
            final DebtAddedEvent event = buildEvent();

            dlqConsumer.consume(event, buildHeaders());

            verify(debtAddedConsumer).consume(event);
        }

        @Test
        @DisplayName("retry fails → exception caught, not propagated")
        void retryFails_exceptionCaughtNotPropagated() {
            final DebtAddedEvent event = buildEvent();
            doThrow(new RuntimeException("Processing failed"))
                    .when(debtAddedConsumer).consume(event);

            assertDoesNotThrow(() -> dlqConsumer.consume(event, buildHeaders()));
        }

        @Test
        @DisplayName("empty headers → no NullPointerException")
        void emptyHeaders_noException() {
            assertDoesNotThrow(() -> dlqConsumer.consume(buildEvent(), Map.of()));
        }
    }
}
