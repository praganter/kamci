package dev.batuhanyetgin.notificationservice.consumer.dlq;

import dev.batuhanyetgin.commonmodule.event.DebtReminderEvent;
import dev.batuhanyetgin.notificationservice.consumer.DebtReminderConsumer;
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
class DebtReminderDlqConsumerTest {

    @Mock private DebtReminderConsumer debtReminderConsumer;

    @InjectMocks
    private DebtReminderDlqConsumer dlqConsumer;

    private DebtReminderEvent buildEvent() {
        return DebtReminderEvent.builder()
                .debtId("debt123")
                .debtName("Test Debt")
                .ownerId("owner1")
                .amount(BigDecimal.TEN)
                .build();
    }

    private Map<String, Object> buildHeaders() {
        return Map.of(
                "x-first-death-reason", "rejected",
                "x-first-death-queue", "debt-reminder-queue"
        );
    }

    @Nested
    @DisplayName("consume")
    class Consume {

        @Test
        @DisplayName("retry succeeds → delegates to main consumer")
        void retrySucceeds_delegatesToMainConsumer() {
            final DebtReminderEvent event = buildEvent();

            dlqConsumer.consume(event, buildHeaders());

            verify(debtReminderConsumer).consume(event);
        }

        @Test
        @DisplayName("retry fails → exception caught, not propagated")
        void retryFails_exceptionCaughtNotPropagated() {
            final DebtReminderEvent event = buildEvent();
            doThrow(new RuntimeException("Processing failed"))
                    .when(debtReminderConsumer).consume(event);

            assertDoesNotThrow(() -> dlqConsumer.consume(event, buildHeaders()));
        }

        @Test
        @DisplayName("empty headers → no NullPointerException")
        void emptyHeaders_noException() {
            assertDoesNotThrow(() -> dlqConsumer.consume(buildEvent(), Map.of()));
        }
    }
}
