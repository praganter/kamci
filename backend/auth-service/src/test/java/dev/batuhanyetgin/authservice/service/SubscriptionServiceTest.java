package dev.batuhanyetgin.authservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.batuhanyetgin.authservice.document.AuthDocument;
import dev.batuhanyetgin.authservice.document.SubscriptionDocument;
import dev.batuhanyetgin.authservice.dto.request.VerifyReceiptRequest;
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
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private AuthRepositoryService authRS;
    @Mock private SubscriptionRepositoryService subscriptionRS;
    @Mock private JwtService jwtService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
    @Mock private AppleJwsVerifier appleJwsVerifier;
    @Mock private GooglePlayVerifier googlePlayVerifier;
    @Mock private AppleReceiptVerifier appleReceiptVerifier;

    private SubscriptionService subscriptionService;

    private static final String USER_ID = "user123";

    @BeforeEach
    void setUp() {
        subscriptionService = new SubscriptionService(authRS, subscriptionRS, jwtService, objectMapper, appleJwsVerifier, googlePlayVerifier, appleReceiptVerifier);
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

    // ─── verifyReceipt ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("verifyReceipt")
    class VerifyReceipt {

        private final ObjectMapper mapper = new ObjectMapper();

        private VerifyReceiptRequest iosRequest() {
            return VerifyReceiptRequest.builder()
                    .platform("IOS").plan("YEARLY")
                    .receiptData("ios-receipt").productId("yearly_ios")
                    .build();
        }

        private VerifyReceiptRequest androidRequest() {
            return VerifyReceiptRequest.builder()
                    .platform("ANDROID").plan("MONTHLY")
                    .receiptData("goog-token").productId("monthly_android")
                    .packageName("com.example.app")
                    .build();
        }

        @Test
        @DisplayName("IOS receipt valid → subscription saved as ACTIVE, tokens returned")
        void ios_validReceipt_subscriptionActivated() {
            final long expiresMs = System.currentTimeMillis() + 86_400_000L;
            final var receiptInfo = mapper.createObjectNode();
            receiptInfo.put("original_transaction_id", "TX_IOS_1");
            receiptInfo.put("expires_date_ms", expiresMs);

            when(appleReceiptVerifier.verify("ios-receipt")).thenReturn(receiptInfo);
            when(subscriptionRS.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(authRS.findById(USER_ID)).thenReturn(buildAuthDoc());
            when(authRS.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(jwtService.generateToken(USER_ID, false, true)).thenReturn("access-token");
            when(jwtService.generateToken(USER_ID, true, false)).thenReturn("refresh-token");

            final SubscriptionResponse response = subscriptionService.verifyReceipt(USER_ID, iosRequest());

            final ArgumentCaptor<SubscriptionDocument> captor = ArgumentCaptor.forClass(SubscriptionDocument.class);
            verify(subscriptionRS).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(captor.getValue().getPlatform()).isEqualTo(SubscriptionPlatform.IOS);
            assertThat(captor.getValue().getOriginalTransactionId()).isEqualTo("TX_IOS_1");
            assertThat(response.getAccessToken()).isEqualTo("access-token");
        }

        @Test
        @DisplayName("ANDROID purchase valid → subscription saved as ACTIVE, tokens returned")
        void android_validPurchase_subscriptionActivated() throws Exception {
            final long expiresMs = System.currentTimeMillis() + 86_400_000L;
            final var purchaseInfo = mapper.createObjectNode();
            purchaseInfo.put("orderId", "GPA.ORDER-1");
            purchaseInfo.put("expiryTimeMillis", expiresMs);

            when(googlePlayVerifier.verifySubscription("com.example.app", "monthly_android", "goog-token"))
                    .thenReturn(purchaseInfo);
            when(subscriptionRS.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(authRS.findById(USER_ID)).thenReturn(buildAuthDoc());
            when(authRS.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(jwtService.generateToken(USER_ID, false, true)).thenReturn("access-token");
            when(jwtService.generateToken(USER_ID, true, false)).thenReturn("refresh-token");

            final SubscriptionResponse response = subscriptionService.verifyReceipt(USER_ID, androidRequest());

            final ArgumentCaptor<SubscriptionDocument> captor = ArgumentCaptor.forClass(SubscriptionDocument.class);
            verify(subscriptionRS).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(captor.getValue().getPlatform()).isEqualTo(SubscriptionPlatform.ANDROID);
            assertThat(captor.getValue().getPurchaseToken()).isEqualTo("goog-token");
            assertThat(response.getAccessToken()).isEqualTo("access-token");
        }

        @Test
        @DisplayName("IOS receipt invalid → throws BAD_REQUEST, subscription not saved")
        void ios_invalidReceipt_throwsBadRequest() {
            when(appleReceiptVerifier.verify("ios-receipt"))
                    .thenThrow(BaseException.badRequest("error.subscription.verification_failed"));

            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> subscriptionService.verifyReceipt(USER_ID, iosRequest()))
                    .matches(e -> e.getStatus() == HttpStatus.BAD_REQUEST);

            verify(subscriptionRS, never()).save(any());
        }

        @Test
        @DisplayName("ANDROID purchase invalid → throws BAD_REQUEST, subscription not saved")
        void android_invalidPurchase_throwsBadRequest() throws Exception {
            when(googlePlayVerifier.verifySubscription(any(), any(), any()))
                    .thenThrow(new RuntimeException("API error"));

            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> subscriptionService.verifyReceipt(USER_ID, androidRequest()))
                    .matches(e -> e.getStatus() == HttpStatus.BAD_REQUEST);

            verify(subscriptionRS, never()).save(any());
        }

        @Test
        @DisplayName("existing subscription updated rather than created")
        void existingSubscription_updatedNotCreated() {
            final long expiresMs = System.currentTimeMillis() + 86_400_000L;
            final var receiptInfo = mapper.createObjectNode();
            receiptInfo.put("original_transaction_id", "TX_IOS_2");
            receiptInfo.put("expires_date_ms", expiresMs);

            final SubscriptionDocument existing = buildActiveSubscription();
            ReflectionTestUtils.setField(existing, "id", "existing-id");
            when(appleReceiptVerifier.verify("ios-receipt")).thenReturn(receiptInfo);
            when(subscriptionRS.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(authRS.findById(USER_ID)).thenReturn(buildAuthDoc());
            when(authRS.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(jwtService.generateToken(any(), anyBoolean(), anyBoolean())).thenReturn("token");

            subscriptionService.verifyReceipt(USER_ID, iosRequest());

            final ArgumentCaptor<SubscriptionDocument> captor = ArgumentCaptor.forClass(SubscriptionDocument.class);
            verify(subscriptionRS).save(captor.capture());
            assertThat(captor.getValue()).isSameAs(existing);
            assertThat(captor.getValue().getStartDate()).isNull();
        }
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

    // ─── handleAppleWebhook ───────────────────────────────────────────────────

    @Nested
    @DisplayName("handleAppleWebhook")
    class HandleAppleWebhook {

        private final ObjectMapper mapper = new ObjectMapper();

        private String buildGoogleBase64(final String json) {
            return Base64.getEncoder().encodeToString(json.getBytes());
        }

        private ObjectNode buildTxInfoNode(final String originalTransactionId, final long expiresDate) {
            final ObjectNode txInfo = mapper.createObjectNode();
            txInfo.put("originalTransactionId", originalTransactionId);
            txInfo.put("expiresDate", expiresDate);
            return txInfo;
        }

        private ObjectNode buildOuterPayload(final String notificationType,
                                             final String signedTransactionInfo,
                                             final String signedRenewalInfo) {
            final ObjectNode data = mapper.createObjectNode();
            data.put("signedTransactionInfo", signedTransactionInfo);
            data.put("signedRenewalInfo", signedRenewalInfo != null ? signedRenewalInfo : "");
            final ObjectNode payload = mapper.createObjectNode();
            payload.put("notificationType", notificationType);
            payload.set("data", data);
            return payload;
        }

        @Test
        @DisplayName("SUBSCRIBED → subscription set to ACTIVE and premium enabled")
        void subscribed_setsActiveAndPremium() throws Exception {
            final String txId = "TX123";
            final long expiresMs = System.currentTimeMillis() + 86_400_000L;
            final ObjectNode txInfo = buildTxInfoNode(txId, expiresMs);
            final ObjectNode outerPayload = buildOuterPayload("SUBSCRIBED", "signed.tx.token", null);

            when(appleJwsVerifier.verifyAndDecode("signed.payload")).thenReturn(outerPayload);
            when(appleJwsVerifier.verifyAndDecode("signed.tx.token")).thenReturn(txInfo);

            final SubscriptionDocument sub = SubscriptionDocument.builder()
                    .userId(USER_ID).status(SubscriptionStatus.EXPIRED).build();
            when(subscriptionRS.findByOriginalTransactionId(txId)).thenReturn(sub);
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));

            final AuthDocument auth = buildAuthDoc();
            when(authRS.findById(USER_ID)).thenReturn(auth);
            when(authRS.save(auth)).thenReturn(auth);

            subscriptionService.handleAppleWebhook("signed.payload");

            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(auth.isPremium()).isTrue();
        }

        @Test
        @DisplayName("EXPIRED → subscription set to EXPIRED and premium disabled")
        void expired_setsExpiredAndRemovesPremium() throws Exception {
            final String txId = "TX456";
            final ObjectNode txInfo = buildTxInfoNode(txId, 0L);
            final ObjectNode outerPayload = buildOuterPayload("EXPIRED", "signed.tx.token", null);

            when(appleJwsVerifier.verifyAndDecode("signed.payload")).thenReturn(outerPayload);
            when(appleJwsVerifier.verifyAndDecode("signed.tx.token")).thenReturn(txInfo);

            final SubscriptionDocument sub = SubscriptionDocument.builder()
                    .userId(USER_ID).status(SubscriptionStatus.ACTIVE).build();
            when(subscriptionRS.findByOriginalTransactionId(txId)).thenReturn(sub);
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));

            final AuthDocument auth = buildAuthDoc();
            auth.setPremium(true);
            when(authRS.findById(USER_ID)).thenReturn(auth);
            when(authRS.save(auth)).thenReturn(auth);

            subscriptionService.handleAppleWebhook("signed.payload");

            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
            assertThat(auth.isPremium()).isFalse();
        }

        @Test
        @DisplayName("REFUND → subscription set to CANCELLED and premium disabled")
        void refund_setsCancelledAndRemovesPremium() throws Exception {
            final String txId = "TX789";
            final ObjectNode txInfo = buildTxInfoNode(txId, 0L);
            final ObjectNode outerPayload = buildOuterPayload("REFUND", "signed.tx.token", null);

            when(appleJwsVerifier.verifyAndDecode("signed.payload")).thenReturn(outerPayload);
            when(appleJwsVerifier.verifyAndDecode("signed.tx.token")).thenReturn(txInfo);

            final SubscriptionDocument sub = SubscriptionDocument.builder()
                    .userId(USER_ID).status(SubscriptionStatus.ACTIVE).build();
            when(subscriptionRS.findByOriginalTransactionId(txId)).thenReturn(sub);
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));

            final AuthDocument auth = buildAuthDoc();
            auth.setPremium(true);
            when(authRS.findById(USER_ID)).thenReturn(auth);
            when(authRS.save(auth)).thenReturn(auth);

            subscriptionService.handleAppleWebhook("signed.payload");

            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
            assertThat(auth.isPremium()).isFalse();
        }

        @Test
        @DisplayName("JWS verification fails → exception swallowed, no DB call")
        void jwsVerificationFails_exceptionSwallowed() throws Exception {
            when(appleJwsVerifier.verifyAndDecode("bad.payload"))
                    .thenThrow(new SecurityException("bad signature"));

            subscriptionService.handleAppleWebhook("bad.payload");

            verifyNoInteractions(subscriptionRS, authRS);
        }
    }

    // ─── handleGoogleWebhook ──────────────────────────────────────────────────

    @Nested
    @DisplayName("handleGoogleWebhook")
    class HandleGoogleWebhook {

        private String base64Json(final String json) {
            return Base64.getEncoder().encodeToString(json.getBytes());
        }

        @Test
        @DisplayName("RENEWED notification → subscription set to ACTIVE and premium enabled")
        void renewed_setsActiveAndPremium() {
            final String purchaseToken = "goog-token-123";
            final String json = "{\"subscriptionNotification\":{\"notificationType\":4,\"purchaseToken\":\"" + purchaseToken + "\"}}";

            final SubscriptionDocument sub = SubscriptionDocument.builder()
                    .userId(USER_ID).status(SubscriptionStatus.EXPIRED).build();
            when(subscriptionRS.findByPurchaseToken(purchaseToken)).thenReturn(sub);
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));

            final AuthDocument auth = buildAuthDoc();
            when(authRS.findById(USER_ID)).thenReturn(auth);
            when(authRS.save(auth)).thenReturn(auth);

            subscriptionService.handleGoogleWebhook(base64Json(json));

            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(auth.isPremium()).isTrue();
        }

        @Test
        @DisplayName("EXPIRED notification → subscription set to EXPIRED and premium disabled")
        void expired_setsExpiredAndRemovesPremium() {
            final String purchaseToken = "goog-token-456";
            final String json = "{\"subscriptionNotification\":{\"notificationType\":13,\"purchaseToken\":\"" + purchaseToken + "\"}}";

            final SubscriptionDocument sub = SubscriptionDocument.builder()
                    .userId(USER_ID).status(SubscriptionStatus.ACTIVE).build();
            when(subscriptionRS.findByPurchaseToken(purchaseToken)).thenReturn(sub);
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));

            final AuthDocument auth = buildAuthDoc();
            auth.setPremium(true);
            when(authRS.findById(USER_ID)).thenReturn(auth);
            when(authRS.save(auth)).thenReturn(auth);

            subscriptionService.handleGoogleWebhook(base64Json(json));

            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
            assertThat(auth.isPremium()).isFalse();
        }

        @Test
        @DisplayName("CANCELED notification → subscription set to CANCELLED")
        void canceled_setsCancelled() {
            final String purchaseToken = "goog-token-789";
            final String json = "{\"subscriptionNotification\":{\"notificationType\":3,\"purchaseToken\":\"" + purchaseToken + "\"}}";

            final SubscriptionDocument sub = SubscriptionDocument.builder()
                    .userId(USER_ID).status(SubscriptionStatus.ACTIVE).build();
            when(subscriptionRS.findByPurchaseToken(purchaseToken)).thenReturn(sub);
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));

            final AuthDocument auth = buildAuthDoc();
            auth.setPremium(true);
            when(authRS.findById(USER_ID)).thenReturn(auth);
            when(authRS.save(auth)).thenReturn(auth);

            subscriptionService.handleGoogleWebhook(base64Json(json));

            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        }

        @Test
        @DisplayName("non-subscription notification (e.g. ONE_TIME_PRODUCT) → no DB call")
        void nonSubscriptionNotification_noDbCall() {
            final String json = "{\"oneTimeProductNotification\":{\"notificationType\":1}}";

            subscriptionService.handleGoogleWebhook(base64Json(json));

            verifyNoInteractions(subscriptionRS, authRS);
        }

        @Test
        @DisplayName("invalid base64 → exception swallowed, no DB call")
        void invalidBase64_exceptionSwallowed() {
            subscriptionService.handleGoogleWebhook("not-valid-base64!!!");

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
