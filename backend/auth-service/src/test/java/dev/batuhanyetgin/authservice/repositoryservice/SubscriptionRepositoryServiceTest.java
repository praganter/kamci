package dev.batuhanyetgin.authservice.repositoryservice;

import dev.batuhanyetgin.authservice.document.SubscriptionDocument;
import dev.batuhanyetgin.authservice.enums.SubscriptionStatus;
import dev.batuhanyetgin.authservice.repository.SubscriptionRepository;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionRepositoryServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;

    private SubscriptionRepositoryService service;

    private static final String USER_ID        = "user123";
    private static final String TRANSACTION_ID = "txn_abc123";
    private static final String PURCHASE_TOKEN = "token_xyz789";

    @BeforeEach
    void setUp() {
        service = new SubscriptionRepositoryService(subscriptionRepository);
    }

    private SubscriptionDocument buildDoc() {
        return SubscriptionDocument.builder()
                .userId(USER_ID)
                .status(SubscriptionStatus.ACTIVE)
                .build();
    }

    // ─── findByUserId ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByUserId")
    class FindByUserId {

        @Test
        @DisplayName("existing userId → returns Optional with document")
        void existingUserId_returnsPresent() {
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(buildDoc()));

            assertThat(service.findByUserId(USER_ID)).isPresent();
        }

        @Test
        @DisplayName("missing userId → returns empty Optional")
        void missingUserId_returnsEmpty() {
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThat(service.findByUserId(USER_ID)).isEmpty();
        }
    }

    // ─── findByOriginalTransactionId ──────────────────────────────────────────

    @Nested
    @DisplayName("findByOriginalTransactionId")
    class FindByOriginalTransactionId {

        @Test
        @DisplayName("existing transaction → returns document")
        void existingTransaction_returnsDocument() {
            final SubscriptionDocument doc = buildDoc();
            when(subscriptionRepository.findByOriginalTransactionId(TRANSACTION_ID))
                    .thenReturn(Optional.of(doc));

            assertThat(service.findByOriginalTransactionId(TRANSACTION_ID)).isEqualTo(doc);
        }

        @Test
        @DisplayName("missing transaction → throws NOT_FOUND")
        void missingTransaction_throwsNotFound() {
            when(subscriptionRepository.findByOriginalTransactionId(TRANSACTION_ID))
                    .thenReturn(Optional.empty());

            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> service.findByOriginalTransactionId(TRANSACTION_ID))
                    .matches(e -> e.getStatus() == HttpStatus.NOT_FOUND);
        }
    }

    // ─── findByPurchaseToken ──────────────────────────────────────────────────

    @Nested
    @DisplayName("findByPurchaseToken")
    class FindByPurchaseToken {

        @Test
        @DisplayName("existing token → returns document")
        void existingToken_returnsDocument() {
            final SubscriptionDocument doc = buildDoc();
            when(subscriptionRepository.findByPurchaseToken(PURCHASE_TOKEN))
                    .thenReturn(Optional.of(doc));

            assertThat(service.findByPurchaseToken(PURCHASE_TOKEN)).isEqualTo(doc);
        }

        @Test
        @DisplayName("missing token → throws NOT_FOUND")
        void missingToken_throwsNotFound() {
            when(subscriptionRepository.findByPurchaseToken(PURCHASE_TOKEN))
                    .thenReturn(Optional.empty());

            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> service.findByPurchaseToken(PURCHASE_TOKEN))
                    .matches(e -> e.getStatus() == HttpStatus.NOT_FOUND);
        }
    }

    // ─── findActiveExpired ────────────────────────────────────────────────────

    @Nested
    @DisplayName("findActiveExpired")
    class FindActiveExpired {

        @Test
        @DisplayName("expired subscriptions exist → returns them")
        void expiredExist_returnsList() {
            final LocalDateTime now = LocalDateTime.now();
            final List<SubscriptionDocument> expired = List.of(buildDoc(), buildDoc());
            when(subscriptionRepository.findByStatusAndExpiryDateBefore(SubscriptionStatus.ACTIVE, now))
                    .thenReturn(expired);

            assertThat(service.findActiveExpired(now)).hasSize(2);
        }

        @Test
        @DisplayName("no expired subscriptions → returns empty list")
        void noneExpired_returnsEmpty() {
            final LocalDateTime now = LocalDateTime.now();
            when(subscriptionRepository.findByStatusAndExpiryDateBefore(SubscriptionStatus.ACTIVE, now))
                    .thenReturn(List.of());

            assertThat(service.findActiveExpired(now)).isEmpty();
        }
    }
}
