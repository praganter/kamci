package dev.batuhanyetgin.notificationservice.consumer.dlq;

import dev.batuhanyetgin.commonmodule.event.CategoryMemberAddedEvent;
import dev.batuhanyetgin.notificationservice.consumer.CategoryMemberAddedConsumer;
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
class CategoryMemberAddedDlqConsumerTest {

    @Mock private CategoryMemberAddedConsumer categoryMemberAddedConsumer;

    @InjectMocks
    private CategoryMemberAddedDlqConsumer dlqConsumer;

    private CategoryMemberAddedEvent buildEvent() {
        return CategoryMemberAddedEvent.builder()
                .categoryId("cat123")
                .categoryName("Test Category")
                .addedByUserId("owner1")
                .memberUserId("member1")
                .locale("en")
                .build();
    }

    private Map<String, Object> buildHeaders() {
        return Map.of(
                "x-first-death-reason", "rejected",
                "x-first-death-queue", "category-member-added-queue"
        );
    }

    @Nested
    @DisplayName("consume")
    class Consume {

        @Test
        @DisplayName("retry succeeds → delegates to main consumer")
        void retrySucceeds_delegatesToMainConsumer() {
            final CategoryMemberAddedEvent event = buildEvent();

            dlqConsumer.consume(event, buildHeaders());

            verify(categoryMemberAddedConsumer).consume(event);
        }

        @Test
        @DisplayName("retry fails → exception caught, not propagated")
        void retryFails_exceptionCaughtNotPropagated() {
            final CategoryMemberAddedEvent event = buildEvent();
            doThrow(new RuntimeException("Processing failed"))
                    .when(categoryMemberAddedConsumer).consume(event);

            assertDoesNotThrow(() -> dlqConsumer.consume(event, buildHeaders()));
        }

        @Test
        @DisplayName("empty headers → no NullPointerException")
        void emptyHeaders_noException() {
            final CategoryMemberAddedEvent event = buildEvent();

            assertDoesNotThrow(() -> dlqConsumer.consume(event, Map.of()));
        }
    }
}
