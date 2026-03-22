package dev.batuhanyetgin.debttracker.service;

import dev.batuhanyetgin.commonmodule.event.BaseEvent;
import dev.batuhanyetgin.commonmodule.event.DebtAddedEvent;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.debttracker.document.CategoryDocument;
import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.document.embedded.MemberEmbedded;
import dev.batuhanyetgin.debttracker.enums.AccessLevelType;
import dev.batuhanyetgin.debttracker.enums.PaymentPeriodType;
import dev.batuhanyetgin.debttracker.producer.DebtEventProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebtEventAsyncPublisherTest {

    @Mock private DebtEventProducer debtEventProducer;

    @InjectMocks
    private DebtEventAsyncPublisher publisher;

    private static final String CREATOR_ID = "creator123";
    private static final String MEMBER_1   = "member1";
    private static final String MEMBER_2   = "member2";

    private DebtDocument buildDebt() {
        return DebtDocument.builder()
                .id("debt1")
                .ownerId(CREATOR_ID)
                .name("Test Debt")
                .amount(BigDecimal.TEN)
                .paymentPeriodType(PaymentPeriodType.MONTHLY)
                .startDate(LocalDateTime.now())
                .build();
    }

    private MemberEmbedded buildMember(final String userId) {
        return MemberEmbedded.builder()
                .userId(userId)
                .accessLevel(AccessLevelType.READ)
                .build();
    }

    private CategoryDocument buildCategory(final List<MemberEmbedded> members) {
        return CategoryDocument.builder()
                .id("cat1")
                .ownerId(CREATOR_ID)
                .name("Category")
                .members(members)
                .build();
    }

    // ─── publishDebtAddedEvent ────────────────────────────────────────────────

    @Nested
    @DisplayName("publishDebtAddedEvent")
    class PublishDebtAddedEvent {

        @Test
        @DisplayName("category has members excluding creator → event produced with correct memberIds")
        void membersExcludingCreator_producesEvent() {
            final CategoryDocument category = buildCategory(
                    List.of(buildMember(MEMBER_1), buildMember(MEMBER_2)));

            publisher.publishDebtAddedEvent(buildDebt(), category, CREATOR_ID, "en");

            final ArgumentCaptor<BaseEvent> captor = ArgumentCaptor.forClass(BaseEvent.class);
            verify(debtEventProducer).produce(
                    eq(RabbitMQConstants.NOTIFICATION_EXCHANGE),
                    eq(RabbitMQConstants.DEBT_ADDED_ROUTING_KEY),
                    captor.capture());

            final DebtAddedEvent event = (DebtAddedEvent) captor.getValue();
            assertThat(event.getMemberUserIds()).containsExactlyInAnyOrder(MEMBER_1, MEMBER_2);
            assertThat(event.getDebtId()).isEqualTo("debt1");
            assertThat(event.getLocale()).isEqualTo("en");
        }

        @Test
        @DisplayName("creator is a member → filtered out from event memberIds")
        void creatorIsMember_filteredOut() {
            final CategoryDocument category = buildCategory(
                    List.of(buildMember(CREATOR_ID), buildMember(MEMBER_1)));

            publisher.publishDebtAddedEvent(buildDebt(), category, CREATOR_ID, "tr");

            final ArgumentCaptor<BaseEvent> captor = ArgumentCaptor.forClass(BaseEvent.class);
            verify(debtEventProducer).produce(anyString(), anyString(), captor.capture());

            final DebtAddedEvent event = (DebtAddedEvent) captor.getValue();
            assertThat(event.getMemberUserIds()).containsExactly(MEMBER_1);
            assertThat(event.getMemberUserIds()).doesNotContain(CREATOR_ID);
        }

        @Test
        @DisplayName("creator is the only member → no event produced")
        void onlyCreatorAsMember_noEvent() {
            final CategoryDocument category = buildCategory(
                    List.of(buildMember(CREATOR_ID)));

            publisher.publishDebtAddedEvent(buildDebt(), category, CREATOR_ID, "en");

            verifyNoInteractions(debtEventProducer);
        }

        @Test
        @DisplayName("no members in category → no event produced")
        void noMembers_noEvent() {
            final CategoryDocument category = buildCategory(List.of());

            publisher.publishDebtAddedEvent(buildDebt(), category, CREATOR_ID, "en");

            verifyNoInteractions(debtEventProducer);
        }

        @Test
        @DisplayName("event contains correct debt fields")
        void event_containsCorrectDebtFields() {
            final CategoryDocument category = buildCategory(List.of(buildMember(MEMBER_1)));
            final DebtDocument debt = buildDebt();

            publisher.publishDebtAddedEvent(debt, category, CREATOR_ID, "en");

            final ArgumentCaptor<BaseEvent> captor = ArgumentCaptor.forClass(BaseEvent.class);
            verify(debtEventProducer).produce(anyString(), anyString(), captor.capture());

            final DebtAddedEvent event = (DebtAddedEvent) captor.getValue();
            assertThat(event.getOwnerId()).isEqualTo(CREATOR_ID);
            assertThat(event.getDebtName()).isEqualTo("Test Debt");
            assertThat(event.getAmount()).isEqualByComparingTo(BigDecimal.TEN);
        }
    }
}