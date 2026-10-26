package dev.batuhanyetgin.authservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.batuhanyetgin.authservice.document.AuthDocument;
import dev.batuhanyetgin.authservice.document.SubscriptionDocument;
import dev.batuhanyetgin.authservice.dto.response.SubscriptionResponse;
import dev.batuhanyetgin.authservice.enums.SubscriptionPlan;
import dev.batuhanyetgin.authservice.enums.SubscriptionPlatform;
import dev.batuhanyetgin.authservice.enums.SubscriptionStatus;
import dev.batuhanyetgin.authservice.repositoryservice.AuthRepositoryService;
import dev.batuhanyetgin.authservice.repositoryservice.SubscriptionRepositoryService;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private AuthRepositoryService authRS;
    @Mock private SubscriptionRepositoryService subscriptionRS;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private SubscriptionService subscriptionService;

    private static final String USER_ID = "user123";

    @BeforeEach
    void setUp() {
        subscriptionService = new SubscriptionService(authRS, subscriptionRS, objectMapper);
    }

    private AuthDocument buildAuthDoc() {
        final AuthDocument doc = AuthDocument.builder().email("user@example.com").build();
        ReflectionTestUtils.setField(doc, "id", USER_ID);
        return doc;
    }

    private SubscriptionDocument buildActiveSubscription() {
        return SubscriptionDocument.builder()
                .userId(USER_ID)
                .plan(SubscriptionPlan.YEARLY)
                .status(SubscriptionStatus.ACTIVE)
                .expiryDate(LocalDateTime.now().plusYears(1))
                .build();
    }

    // ─── getStatus ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getStatus")
    class GetStatus {

        @Test
        @DisplayName("subscription exists → returns response with plan and status")
        void subscriptionExists_returnsResponse() {
            when(subscriptionRS.findByUserId(USER_ID)).thenReturn(Optional.of(buildActiveSubscription()));

            final SubscriptionResponse response = subscriptionService.getStatus(USER_ID);

            assertThat(response.getPlan()).isEqualTo(SubscriptionPlan.YEARLY);
            assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        @DisplayName("no subscription → throws NOT_FOUND")
        void noSubscription_throwsNotFound() {
            when(subscriptionRS.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> subscriptionService.getStatus(USER_ID))
                    .matches(e -> e.getStatus() == HttpStatus.NOT_FOUND);
        }
    }

    // ─── grantPremium ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("grantPremium")
    class GrantPremium {

        @Test
        @DisplayName("no existing subscription → creates new one with ACTIVE status")
        void noExistingSubscription_createsNewActive() {
            final AuthDocument auth = buildAuthDoc();
            when(authRS.findById(USER_ID)).thenReturn(auth);
            when(subscriptionRS.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(authRS.save(auth)).thenReturn(auth);

            subscriptionService.grantPremium(USER_ID);

            assertThat(auth.isPremium()).isTrue();
            final ArgumentCaptor<SubscriptionDocument> captor = ArgumentCaptor.forClass(SubscriptionDocument.class);
            verify(subscriptionRS).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(captor.getValue().getPlan()).isEqualTo(SubscriptionPlan.YEARLY);
            assertThat(captor.getValue().getPlatform()).isEqualTo(SubscriptionPlatform.ADMIN);
        }

        @Test
        @DisplayName("existing subscription → updates it to ACTIVE")
        void existingSubscription_updatesStatusToActive() {
            final AuthDocument auth = buildAuthDoc();
            final SubscriptionDocument existing = SubscriptionDocument.builder()
                    .userId(USER_ID).status(SubscriptionStatus.EXPIRED).build();
            when(authRS.findById(USER_ID)).thenReturn(auth);
            when(subscriptionRS.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(authRS.save(auth)).thenReturn(auth);

            subscriptionService.grantPremium(USER_ID);

            assertThat(existing.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }
    }

    // ─── handleRevenueCatWebhook ──────────────────────────────────────────────

    @Nested
    @DisplayName("handleRevenueCatWebhook")
    class HandleRevenueCatWebhook {

        private final ObjectMapper mapper = new ObjectMapper();

        private ObjectNode buildEvent(final String type, final String appUserId,
                                       final String productId, final String store, final long expiresMs) {
            final ObjectNode event = mapper.createObjectNode();
            event.put("type", type);
            event.put("app_user_id", appUserId);
            if (productId != null) event.put("product_id", productId);
            if (store != null) event.put("store", store);
            if (expiresMs > 0) event.put("expiration_at_ms", expiresMs);
            final ObjectNode payload = mapper.createObjectNode();
            payload.set("event", event);
            return payload;
        }

        @Test
        @DisplayName("INITIAL_PURCHASE → subscription created ACTIVE, premium enabled")
        void initialPurchase_createsActiveSubscription() {
            final long expiresMs = System.currentTimeMillis() + 86_400_000L;
            final ObjectNode payload = buildEvent("INITIAL_PURCHASE", USER_ID, "yearly_product", "APP_STORE", expiresMs);

            when(subscriptionRS.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(authRS.findById(USER_ID)).thenReturn(buildAuthDoc());
            when(authRS.save(any())).thenAnswer(inv -> inv.getArgument(0));

            subscriptionService.handleRevenueCatWebhook(payload);

            final ArgumentCaptor<SubscriptionDocument> captor = ArgumentCaptor.forClass(SubscriptionDocument.class);
            verify(subscriptionRS).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(captor.getValue().getPlatform()).isEqualTo(SubscriptionPlatform.IOS);
            assertThat(captor.getValue().getProductId()).isEqualTo("yearly_product");
        }

        @Test
        @DisplayName("RENEWAL → existing subscription set to ACTIVE and premium enabled")
        void renewal_setsActiveAndPremium() {
            final long expiresMs = System.currentTimeMillis() + 86_400_000L;
            final ObjectNode payload = buildEvent("RENEWAL", USER_ID, "monthly_product", "PLAY_STORE", expiresMs);

            final SubscriptionDocument existing = SubscriptionDocument.builder()
                    .userId(USER_ID).status(SubscriptionStatus.EXPIRED).build();
            when(subscriptionRS.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));

            final AuthDocument auth = buildAuthDoc();
            when(authRS.findById(USER_ID)).thenReturn(auth);
            when(authRS.save(auth)).thenReturn(auth);

            subscriptionService.handleRevenueCatWebhook(payload);

            assertThat(existing.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(auth.isPremium()).isTrue();
        }

        @Test
        @DisplayName("EXPIRATION → subscription set to EXPIRED and premium disabled")
        void expiration_setsExpiredAndRemovesPremium() {
            final ObjectNode payload = buildEvent("EXPIRATION", USER_ID, null, null, 0);

            final SubscriptionDocument existing = SubscriptionDocument.builder()
                    .userId(USER_ID).status(SubscriptionStatus.ACTIVE).build();
            when(subscriptionRS.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));

            final AuthDocument auth = buildAuthDoc();
            auth.setPremium(true);
            when(authRS.findById(USER_ID)).thenReturn(auth);
            when(authRS.save(auth)).thenReturn(auth);

            subscriptionService.handleRevenueCatWebhook(payload);

            assertThat(existing.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
            assertThat(auth.isPremium()).isFalse();
        }

        @Test
        @DisplayName("CANCELLATION → subscription saved but premium left untouched")
        void cancellation_doesNotRevokePremiumImmediately() {
            final ObjectNode payload = buildEvent("CANCELLATION", USER_ID, null, null, 0);

            final SubscriptionDocument existing = SubscriptionDocument.builder()
                    .userId(USER_ID).status(SubscriptionStatus.ACTIVE).build();
            when(subscriptionRS.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));

            subscriptionService.handleRevenueCatWebhook(payload);

            verify(subscriptionRS).save(existing);
            verifyNoInteractions(authRS);
        }

        @Test
        @DisplayName("missing app_user_id → no DB call")
        void missingAppUserId_noDbCall() {
            final ObjectNode payload = buildEvent("INITIAL_PURCHASE", "", null, null, 0);

            subscriptionService.handleRevenueCatWebhook(payload);

            verifyNoInteractions(subscriptionRS, authRS);
        }
    }

    // ─── revokePremium ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("revokePremium")
    class RevokePremium {

        @Test
        @DisplayName("subscription exists → sets CANCELLED, removes premium from auth")
        void subscriptionExists_cancelledAndPremiumRemoved() {
            final AuthDocument auth = buildAuthDoc();
            auth.setPremium(true);
            final SubscriptionDocument sub = buildActiveSubscription();

            when(authRS.findById(USER_ID)).thenReturn(auth);
            when(subscriptionRS.findByUserId(USER_ID)).thenReturn(Optional.of(sub));
            when(authRS.save(auth)).thenReturn(auth);
            when(subscriptionRS.save(sub)).thenReturn(sub);

            subscriptionService.revokePremium(USER_ID);

            assertThat(auth.isPremium()).isFalse();
            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        }

        @Test
        @DisplayName("no subscription → only removes premium from auth, no exception")
        void noSubscription_onlyRemovesPremium() {
            final AuthDocument auth = buildAuthDoc();
            auth.setPremium(true);

            when(authRS.findById(USER_ID)).thenReturn(auth);
            when(subscriptionRS.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(authRS.save(auth)).thenReturn(auth);

            assertThatNoException().isThrownBy(() -> subscriptionService.revokePremium(USER_ID));
            assertThat(auth.isPremium()).isFalse();
            verify(subscriptionRS, never()).save(any());
        }
    }
}
