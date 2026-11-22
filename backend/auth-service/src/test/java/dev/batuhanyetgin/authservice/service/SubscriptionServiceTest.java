package dev.batuhanyetgin.authservice.service;

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
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private AuthRepositoryService authRS;
    @Mock private SubscriptionRepositoryService subscriptionRS;
    @Mock private JwtService jwtService;
    @Mock private AppleJwsVerifier appleJwsVerifier;
    @Mock private GooglePlayVerifier googlePlayVerifier;
    @Mock private AppleReceiptVerifier appleReceiptVerifier;
    @Mock private PubSubTokenVerifier pubSubTokenVerifier;

    private final JsonMapper mapper = new JsonMapper();

    private IapSettings settings;
    private SubscriptionService subscriptionService;

    private static final String USER_ID = "user123";
    private static final String BUNDLE = "com.example.kamci";
    private static final String PACKAGE = "com.example.app";

    @BeforeEach
    void setUp() {
        settings = new IapSettings(BUNDLE, false, "monthly_ios,monthly_android", "yearly_ios,yearly_android",
                PACKAGE, "pubsub-audience", "push@example.iam.test");
        subscriptionService = newService(settings);
    }

    private SubscriptionService newService(final IapSettings iapSettings) {
        return new SubscriptionService(authRS, subscriptionRS, jwtService, mapper, appleJwsVerifier,
                googlePlayVerifier, appleReceiptVerifier, pubSubTokenVerifier, iapSettings);
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

    private static void assertFails(final HttpStatus status, final String code, final Runnable call) {
        assertThatExceptionOfType(BaseException.class)
                .isThrownBy(call::run)
                .matches(e -> e.getStatus() == status, "status " + status)
                .withMessage(code);
    }

    // ─── verifyReceipt ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("verifyReceipt")
    class VerifyReceipt {

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
                    .packageName(PACKAGE)
                    .build();
        }

        private ObjectNode iosReceipt(final String productId, final String txId, final long expiresMs) {
            final ObjectNode receiptInfo = mapper.createObjectNode();
            receiptInfo.put("product_id", productId);
            receiptInfo.put("original_transaction_id", txId);
            receiptInfo.put("expires_date_ms", expiresMs);
            return receiptInfo;
        }

        private void stubSaveAndPremium() {
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(authRS.findById(USER_ID)).thenReturn(buildAuthDoc());
            when(authRS.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(jwtService.generateToken(any(), anyBoolean(), anyBoolean())).thenReturn("token");
        }

        @Test
        @DisplayName("IOS receipt valid → subscription saved as ACTIVE, tokens returned")
        void ios_validReceipt_subscriptionActivated() {
            final long expiresMs = System.currentTimeMillis() + 86_400_000L;
            when(appleReceiptVerifier.isConfigured()).thenReturn(true);
            when(appleReceiptVerifier.verify("ios-receipt")).thenReturn(iosReceipt("yearly_ios", "TX_IOS_1", expiresMs));
            when(subscriptionRS.findOptionalByOriginalTransactionId("TX_IOS_1")).thenReturn(Optional.empty());
            when(subscriptionRS.findByUserId(USER_ID)).thenReturn(Optional.empty());
            stubSaveAndPremium();
            when(jwtService.generateToken(USER_ID, false, true)).thenReturn("access-token");
            when(jwtService.generateToken(USER_ID, true, false)).thenReturn("refresh-token");

            final SubscriptionResponse response = subscriptionService.verifyReceipt(USER_ID, iosRequest());

            final ArgumentCaptor<SubscriptionDocument> captor = ArgumentCaptor.forClass(SubscriptionDocument.class);
            verify(subscriptionRS).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(captor.getValue().getPlatform()).isEqualTo(SubscriptionPlatform.IOS);
            assertThat(captor.getValue().getPlan()).isEqualTo(SubscriptionPlan.YEARLY);
            assertThat(captor.getValue().getOriginalTransactionId()).isEqualTo("TX_IOS_1");
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        }

        @Test
        @DisplayName("Apple verification not configured → 503 apple_not_configured, receipt not sent")
        void ios_notConfigured_serviceUnavailable() {
            when(appleReceiptVerifier.isConfigured()).thenReturn(false);

            assertFails(HttpStatus.SERVICE_UNAVAILABLE, "error.subscription.apple_not_configured",
                    () -> subscriptionService.verifyReceipt(USER_ID, iosRequest()));

            verify(appleReceiptVerifier, never()).verify(any());
            verify(subscriptionRS, never()).save(any());
        }

        @Test
        @DisplayName("no product ids configured → 503 for both platforms")
        void productsNotConfigured_serviceUnavailable() {
            final SubscriptionService unconfigured = newService(new IapSettings(BUNDLE, false, "", "", "", "", ""));

            assertFails(HttpStatus.SERVICE_UNAVAILABLE, "error.subscription.apple_not_configured",
                    () -> unconfigured.verifyReceipt(USER_ID, iosRequest()));
            assertFails(HttpStatus.SERVICE_UNAVAILABLE, "error.subscription.google_not_configured",
                    () -> unconfigured.verifyReceipt(USER_ID, androidRequest()));
        }

        @Test
        @DisplayName("unknown product id → 400 product_not_allowed")
        void unknownProduct_badRequest() {
            final VerifyReceiptRequest request = iosRequest();
            request.setProductId("someone_elses_product");

            assertFails(HttpStatus.BAD_REQUEST, "error.subscription.product_not_allowed",
                    () -> subscriptionService.verifyReceipt(USER_ID, request));

            verifyNoInteractions(appleReceiptVerifier);
        }

        @Test
        @DisplayName("monthly product claimed as the yearly plan → 400 product_not_allowed")
        void planMismatch_badRequest() {
            final VerifyReceiptRequest request = iosRequest();
            request.setProductId("monthly_ios");

            assertFails(HttpStatus.BAD_REQUEST, "error.subscription.product_not_allowed",
                    () -> subscriptionService.verifyReceipt(USER_ID, request));
        }

        @Test
        @DisplayName("receipt is for another product than requested → 400 verification_failed")
        void receiptProductMismatch_badRequest() {
            when(appleReceiptVerifier.isConfigured()).thenReturn(true);
            when(appleReceiptVerifier.verify("ios-receipt"))
                    .thenReturn(iosReceipt("monthly_ios", "TX", System.currentTimeMillis() + 86_400_000L));

            assertFails(HttpStatus.BAD_REQUEST, "error.subscription.verification_failed",
                    () -> subscriptionService.verifyReceipt(USER_ID, iosRequest()));

            verify(subscriptionRS, never()).save(any());
        }

        @Test
        @DisplayName("already expired receipt → 400 verification_failed, premium not granted")
        void expiredReceipt_badRequest() {
            when(appleReceiptVerifier.isConfigured()).thenReturn(true);
            when(appleReceiptVerifier.verify("ios-receipt"))
                    .thenReturn(iosReceipt("yearly_ios", "TX", System.currentTimeMillis() - 1_000L));
            when(subscriptionRS.findOptionalByOriginalTransactionId("TX")).thenReturn(Optional.empty());

            assertFails(HttpStatus.BAD_REQUEST, "error.subscription.verification_failed",
                    () -> subscriptionService.verifyReceipt(USER_ID, iosRequest()));

            verify(subscriptionRS, never()).save(any());
            verifyNoInteractions(authRS);
        }

        @Test
        @DisplayName("receipt already bound to another account → 409 already_used")
        void receiptOfAnotherUser_conflict() {
            final SubscriptionDocument other = SubscriptionDocument.builder().userId("someone-else").build();
            when(appleReceiptVerifier.isConfigured()).thenReturn(true);
            when(appleReceiptVerifier.verify("ios-receipt"))
                    .thenReturn(iosReceipt("yearly_ios", "TX_SHARED", System.currentTimeMillis() + 86_400_000L));
            when(subscriptionRS.findOptionalByOriginalTransactionId("TX_SHARED")).thenReturn(Optional.of(other));

            assertFails(HttpStatus.CONFLICT, "error.subscription.already_used",
                    () -> subscriptionService.verifyReceipt(USER_ID, iosRequest()));

            verify(subscriptionRS, never()).save(any());
        }

        @Test
        @DisplayName("IOS receipt invalid → verifier's BAD_REQUEST propagates, subscription not saved")
        void ios_invalidReceipt_throwsBadRequest() {
            when(appleReceiptVerifier.isConfigured()).thenReturn(true);
            when(appleReceiptVerifier.verify("ios-receipt"))
                    .thenThrow(BaseException.badRequest("error.subscription.verification_failed"));

            assertFails(HttpStatus.BAD_REQUEST, "error.subscription.verification_failed",
                    () -> subscriptionService.verifyReceipt(USER_ID, iosRequest()));

            verify(subscriptionRS, never()).save(any());
        }

        @Test
        @DisplayName("existing subscription updated rather than created")
        void existingSubscription_updatedNotCreated() {
            final SubscriptionDocument existing = buildActiveSubscription();
            ReflectionTestUtils.setField(existing, "id", "existing-id");
            when(appleReceiptVerifier.isConfigured()).thenReturn(true);
            when(appleReceiptVerifier.verify("ios-receipt"))
                    .thenReturn(iosReceipt("yearly_ios", "TX_IOS_2", System.currentTimeMillis() + 86_400_000L));
            when(subscriptionRS.findOptionalByOriginalTransactionId("TX_IOS_2")).thenReturn(Optional.of(existing));
            when(subscriptionRS.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
            stubSaveAndPremium();

            subscriptionService.verifyReceipt(USER_ID, iosRequest());

            final ArgumentCaptor<SubscriptionDocument> captor = ArgumentCaptor.forClass(SubscriptionDocument.class);
            verify(subscriptionRS).save(captor.capture());
            assertThat(captor.getValue()).isSameAs(existing);
            assertThat(captor.getValue().getStartDate()).isNull();
        }

        @Test
        @DisplayName("ANDROID purchase valid → subscription saved as ACTIVE, tokens returned")
        void android_validPurchase_subscriptionActivated() throws Exception {
            final var purchaseInfo = mapper.createObjectNode();
            purchaseInfo.put("orderId", "GPA.1234");
            purchaseInfo.put("expiryTimeMillis", System.currentTimeMillis() + 86_400_000L);
            when(googlePlayVerifier.isConfigured()).thenReturn(true);
            when(googlePlayVerifier.verifySubscription(PACKAGE, "monthly_android", "goog-token"))
                    .thenReturn(purchaseInfo);
            when(subscriptionRS.findOptionalByPurchaseToken("goog-token")).thenReturn(Optional.empty());
            when(subscriptionRS.findByUserId(USER_ID)).thenReturn(Optional.empty());
            stubSaveAndPremium();

            subscriptionService.verifyReceipt(USER_ID, androidRequest());

            final ArgumentCaptor<SubscriptionDocument> captor = ArgumentCaptor.forClass(SubscriptionDocument.class);
            verify(subscriptionRS).save(captor.capture());
            assertThat(captor.getValue().getPlatform()).isEqualTo(SubscriptionPlatform.ANDROID);
            assertThat(captor.getValue().getPurchaseToken()).isEqualTo("goog-token");
            assertThat(captor.getValue().getOriginalTransactionId()).isEqualTo("GPA.1234");
        }

        @Test
        @DisplayName("ANDROID purchase invalid → throws BAD_REQUEST, subscription not saved")
        void android_invalidPurchase_throwsBadRequest() throws Exception {
            when(googlePlayVerifier.isConfigured()).thenReturn(true);
            when(googlePlayVerifier.verifySubscription(any(), any(), any()))
                    .thenThrow(new RuntimeException("API error"));

            assertFails(HttpStatus.BAD_REQUEST, "error.subscription.verification_failed",
                    () -> subscriptionService.verifyReceipt(USER_ID, androidRequest()));

            verify(subscriptionRS, never()).save(any());
        }
    }

    @Nested
    @DisplayName("verifyReceipt for ANDROID: package and configuration")
    class VerifyAndroidPackage {

        private VerifyReceiptRequest request(final String packageName) {
            return VerifyReceiptRequest.builder()
                    .platform("ANDROID").plan("MONTHLY")
                    .receiptData("goog-token").productId("monthly_android")
                    .packageName(packageName)
                    .build();
        }

        @Test
        @DisplayName("package name outside the allowlist → 400 package_not_allowed, Google never called")
        void foreignPackage_badRequest() throws Exception {
            when(googlePlayVerifier.isConfigured()).thenReturn(true);

            assertFails(HttpStatus.BAD_REQUEST, "error.subscription.package_not_allowed",
                    () -> subscriptionService.verifyReceipt(USER_ID, request("com.attacker.app")));

            verify(googlePlayVerifier, never()).verifySubscription(any(), any(), any());
            verify(subscriptionRS, never()).save(any());
        }

        @Test
        @DisplayName("package name omitted → the configured package is used")
        void missingPackage_usesConfigured() throws Exception {
            final var purchaseInfo = mapper.createObjectNode();
            purchaseInfo.put("orderId", "GPA.1");
            purchaseInfo.put("expiryTimeMillis", System.currentTimeMillis() + 86_400_000L);
            when(googlePlayVerifier.isConfigured()).thenReturn(true);
            when(googlePlayVerifier.verifySubscription(PACKAGE, "monthly_android", "goog-token")).thenReturn(purchaseInfo);
            when(subscriptionRS.findOptionalByPurchaseToken("goog-token")).thenReturn(Optional.empty());
            when(subscriptionRS.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(authRS.findById(USER_ID)).thenReturn(buildAuthDoc());
            when(authRS.save(any())).thenAnswer(inv -> inv.getArgument(0));

            subscriptionService.verifyReceipt(USER_ID, request(null));

            verify(googlePlayVerifier).verifySubscription(PACKAGE, "monthly_android", "goog-token");
        }

        @Test
        @DisplayName("no Play credentials → 503 google_not_configured")
        void noCredentials_serviceUnavailable() {
            when(googlePlayVerifier.isConfigured()).thenReturn(false);

            assertFails(HttpStatus.SERVICE_UNAVAILABLE, "error.subscription.google_not_configured",
                    () -> subscriptionService.verifyReceipt(USER_ID, request(PACKAGE)));
        }

        @Test
        @DisplayName("no GOOGLE_PLAY_PACKAGE_NAME → 503 google_not_configured")
        void noPackageConfigured_serviceUnavailable() {
            final SubscriptionService noPackage = newService(
                    new IapSettings(BUNDLE, false, "monthly_android", "", "", "aud", "push@example.iam.test"));

            assertFails(HttpStatus.SERVICE_UNAVAILABLE, "error.subscription.google_not_configured",
                    () -> noPackage.verifyReceipt(USER_ID, request(PACKAGE)));
        }

        @Test
        @DisplayName("purchase token already bound to another account → 409 already_used")
        void tokenOfAnotherUser_conflict() throws Exception {
            final var purchaseInfo = mapper.createObjectNode();
            purchaseInfo.put("orderId", "GPA.1");
            purchaseInfo.put("expiryTimeMillis", System.currentTimeMillis() + 86_400_000L);
            when(googlePlayVerifier.isConfigured()).thenReturn(true);
            when(googlePlayVerifier.verifySubscription(PACKAGE, "monthly_android", "goog-token")).thenReturn(purchaseInfo);
            when(subscriptionRS.findOptionalByPurchaseToken("goog-token"))
                    .thenReturn(Optional.of(SubscriptionDocument.builder().userId("someone-else").build()));

            assertFails(HttpStatus.CONFLICT, "error.subscription.already_used",
                    () -> subscriptionService.verifyReceipt(USER_ID, request(PACKAGE)));

            verify(subscriptionRS, never()).save(any());
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
            assertThat(response.getAccessToken()).isNull();
        }

        @Test
        @DisplayName("no subscription → throws NOT_FOUND")
        void noSubscription_throwsNotFound() {
            when(subscriptionRS.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertFails(HttpStatus.NOT_FOUND, "error.subscription.not_found",
                    () -> subscriptionService.getStatus(USER_ID));
        }
    }

    // ─── grantPremium / revokePremium ─────────────────────────────────────────

    @Nested
    @DisplayName("grantPremium")
    class GrantPremium {

        @Test
        @DisplayName("no existing subscription → creates new one with ACTIVE status")
        void noSubscription_createsActive() {
            final AuthDocument auth = buildAuthDoc();
            when(authRS.findById(USER_ID)).thenReturn(auth);
            when(authRS.save(auth)).thenReturn(auth);
            when(subscriptionRS.findByUserId(USER_ID)).thenReturn(Optional.empty());

            subscriptionService.grantPremium(USER_ID);

            final ArgumentCaptor<SubscriptionDocument> captor = ArgumentCaptor.forClass(SubscriptionDocument.class);
            verify(subscriptionRS).save(captor.capture());
            assertThat(auth.isPremium()).isTrue();
            assertThat(captor.getValue().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(captor.getValue().getPlatform()).isEqualTo(SubscriptionPlatform.ADMIN);
        }

        @Test
        @DisplayName("existing subscription → updates it to ACTIVE")
        void existingSubscription_updatesToActive() {
            final AuthDocument auth = buildAuthDoc();
            final SubscriptionDocument existing = SubscriptionDocument.builder()
                    .userId(USER_ID).status(SubscriptionStatus.EXPIRED).build();
            when(authRS.findById(USER_ID)).thenReturn(auth);
            when(authRS.save(auth)).thenReturn(auth);
            when(subscriptionRS.findByUserId(USER_ID)).thenReturn(Optional.of(existing));

            subscriptionService.grantPremium(USER_ID);

            assertThat(existing.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("revokePremium")
    class RevokePremium {

        @Test
        @DisplayName("subscription exists → sets CANCELLED, removes premium from auth")
        void subscriptionExists_cancelsAndRemovesPremium() {
            final AuthDocument auth = buildAuthDoc();
            auth.setPremium(true);
            final SubscriptionDocument sub = buildActiveSubscription();
            when(authRS.findById(USER_ID)).thenReturn(auth);
            when(authRS.save(auth)).thenReturn(auth);
            when(subscriptionRS.findByUserId(USER_ID)).thenReturn(Optional.of(sub));

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
            when(authRS.save(auth)).thenReturn(auth);
            when(subscriptionRS.findByUserId(USER_ID)).thenReturn(Optional.empty());

            subscriptionService.revokePremium(USER_ID);

            assertThat(auth.isPremium()).isFalse();
            verify(subscriptionRS, never()).save(any());
        }
    }

    // ─── Apple webhook ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("handleAppleWebhook")
    class HandleAppleWebhook {

        private static final String TX_ID = "TX123";
        private static final long SIGNED_AT = 1_800_000_000_000L;

        private ObjectNode payload(final String type, final String environment, final String bundleId, final Long signedDate) {
            final ObjectNode data = mapper.createObjectNode();
            data.put("environment", environment);
            data.put("bundleId", bundleId);
            data.put("signedTransactionInfo", "signed.tx");
            data.put("signedRenewalInfo", "signed.renewal");
            final ObjectNode payload = mapper.createObjectNode();
            payload.put("notificationType", type);
            if (signedDate != null) {
                payload.put("signedDate", signedDate);
            }
            payload.set("data", data);
            return payload;
        }

        private ObjectNode tx(final String environment, final String bundleId, final long expiresDate) {
            final ObjectNode tx = mapper.createObjectNode();
            tx.put("originalTransactionId", TX_ID);
            tx.put("expiresDate", expiresDate);
            tx.put("bundleId", bundleId);
            tx.put("environment", environment);
            return tx;
        }

        private void stubValidNotification(final String type) {
            when(appleJwsVerifier.verifyAndDecode("signed.payload")).thenReturn(payload(type, "Production", BUNDLE, SIGNED_AT));
            when(appleJwsVerifier.verifyAndDecode("signed.tx")).thenReturn(tx("Production", BUNDLE, 0L));
        }

        private SubscriptionDocument existingSub(final SubscriptionStatus status) {
            final SubscriptionDocument sub = SubscriptionDocument.builder().userId(USER_ID).status(status).build();
            when(subscriptionRS.findOptionalByOriginalTransactionId(TX_ID)).thenReturn(Optional.of(sub));
            return sub;
        }

        private AuthDocument stubAuth() {
            final AuthDocument auth = buildAuthDoc();
            when(authRS.findById(USER_ID)).thenReturn(auth);
            when(authRS.save(auth)).thenReturn(auth);
            return auth;
        }

        @Test
        @DisplayName("SUBSCRIBED → ACTIVE, expiry from the transaction, premium on, notification time recorded")
        void subscribed_setsActiveAndPremium() {
            final long expiresMs = System.currentTimeMillis() + 86_400_000L;
            when(appleJwsVerifier.verifyAndDecode("signed.payload")).thenReturn(payload("SUBSCRIBED", "Production", BUNDLE, SIGNED_AT));
            when(appleJwsVerifier.verifyAndDecode("signed.tx")).thenReturn(tx("Production", BUNDLE, expiresMs));
            final SubscriptionDocument sub = existingSub(SubscriptionStatus.EXPIRED);
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));
            final AuthDocument auth = stubAuth();

            subscriptionService.handleAppleWebhook("signed.payload");

            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(sub.getExpiryDate()).isNotNull();
            assertThat(sub.getLastNotificationMs()).isEqualTo(SIGNED_AT);
            assertThat(auth.isPremium()).isTrue();
        }

        @Test
        @DisplayName("EXPIRED → EXPIRED and premium off")
        void expired_setsExpiredAndRemovesPremium() {
            stubValidNotification("EXPIRED");
            final SubscriptionDocument sub = existingSub(SubscriptionStatus.ACTIVE);
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));
            final AuthDocument auth = stubAuth();
            auth.setPremium(true);

            subscriptionService.handleAppleWebhook("signed.payload");

            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
            assertThat(auth.isPremium()).isFalse();
        }

        @Test
        @DisplayName("REFUND → CANCELLED and premium off")
        void refund_setsCancelledAndRemovesPremium() {
            stubValidNotification("REFUND");
            final SubscriptionDocument sub = existingSub(SubscriptionStatus.ACTIVE);
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));
            final AuthDocument auth = stubAuth();
            auth.setPremium(true);

            subscriptionService.handleAppleWebhook("signed.payload");

            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
            assertThat(auth.isPremium()).isFalse();
        }

        @Test
        @DisplayName("GRACE_PERIOD_EXPIRED → EXPIRED and premium off")
        void gracePeriodExpired_setsExpired() {
            stubValidNotification("GRACE_PERIOD_EXPIRED");
            final SubscriptionDocument sub = existingSub(SubscriptionStatus.ACTIVE);
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));
            final AuthDocument auth = stubAuth();
            auth.setPremium(true);

            subscriptionService.handleAppleWebhook("signed.payload");

            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
            assertThat(auth.isPremium()).isFalse();
        }

        @Test
        @DisplayName("DID_CHANGE_RENEWAL_STATUS → status untouched, notification time recorded")
        void didChangeRenewalStatus_onlyRecordsTime() {
            stubValidNotification("DID_CHANGE_RENEWAL_STATUS");
            when(appleJwsVerifier.verifyAndDecode("signed.renewal")).thenReturn(mapper.createObjectNode().put("autoRenewStatus", 0));
            final SubscriptionDocument sub = existingSub(SubscriptionStatus.ACTIVE);

            subscriptionService.handleAppleWebhook("signed.payload");

            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(sub.getLastNotificationMs()).isEqualTo(SIGNED_AT);
            verify(subscriptionRS).save(sub);
            verifyNoInteractions(authRS);
        }

        @Test
        @DisplayName("unhandled notification type → nothing saved")
        void unhandledType_ignored() {
            stubValidNotification("PRICE_INCREASE");
            existingSub(SubscriptionStatus.ACTIVE);

            subscriptionService.handleAppleWebhook("signed.payload");

            verify(subscriptionRS, never()).save(any());
        }

        @Test
        @DisplayName("replay: same or older signedDate than the last applied one → ignored")
        void staleNotification_ignored() {
            stubValidNotification("EXPIRED");
            final SubscriptionDocument sub = existingSub(SubscriptionStatus.ACTIVE);
            sub.setLastNotificationMs(SIGNED_AT);

            subscriptionService.handleAppleWebhook("signed.payload");

            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            verify(subscriptionRS, never()).save(any());
            verifyNoInteractions(authRS);
        }

        @Test
        @DisplayName("newer signedDate than the last applied one → applied")
        void newerNotification_applied() {
            stubValidNotification("EXPIRED");
            final SubscriptionDocument sub = existingSub(SubscriptionStatus.ACTIVE);
            sub.setLastNotificationMs(SIGNED_AT - 1);
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));
            stubAuth();

            subscriptionService.handleAppleWebhook("signed.payload");

            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        }

        @Test
        @DisplayName("invalid signature → 403, nothing read or written")
        void invalidSignature_forbidden() {
            when(appleJwsVerifier.verifyAndDecode("forged")).thenThrow(new SecurityException("bad signature"));

            assertFails(HttpStatus.FORBIDDEN, "error.subscription.webhook_unauthorized",
                    () -> subscriptionService.handleAppleWebhook("forged"));

            verifyNoInteractions(subscriptionRS, authRS);
        }

        @Test
        @DisplayName("notification for another app's bundle id → 403")
        void otherBundle_forbidden() {
            when(appleJwsVerifier.verifyAndDecode("signed.payload"))
                    .thenReturn(payload("SUBSCRIBED", "Production", "com.other.app", SIGNED_AT));

            assertFails(HttpStatus.FORBIDDEN, "error.subscription.webhook_unauthorized",
                    () -> subscriptionService.handleAppleWebhook("signed.payload"));

            verifyNoInteractions(subscriptionRS, authRS);
        }

        @Test
        @DisplayName("transaction signed for another bundle id → 403")
        void transactionOtherBundle_forbidden() {
            when(appleJwsVerifier.verifyAndDecode("signed.payload")).thenReturn(payload("SUBSCRIBED", "Production", BUNDLE, SIGNED_AT));
            when(appleJwsVerifier.verifyAndDecode("signed.tx")).thenReturn(tx("Production", "com.other.app", 0L));

            assertFails(HttpStatus.FORBIDDEN, "error.subscription.webhook_unauthorized",
                    () -> subscriptionService.handleAppleWebhook("signed.payload"));

            verifyNoInteractions(subscriptionRS, authRS);
        }

        @Test
        @DisplayName("transaction environment differs from the notification's → 403")
        void environmentMismatch_forbidden() {
            when(appleJwsVerifier.verifyAndDecode("signed.payload")).thenReturn(payload("SUBSCRIBED", "Production", BUNDLE, SIGNED_AT));
            when(appleJwsVerifier.verifyAndDecode("signed.tx")).thenReturn(tx("Sandbox", BUNDLE, 0L));

            assertFails(HttpStatus.FORBIDDEN, "error.subscription.webhook_unauthorized",
                    () -> subscriptionService.handleAppleWebhook("signed.payload"));
        }

        @Test
        @DisplayName("unknown environment → 403")
        void unknownEnvironment_forbidden() {
            when(appleJwsVerifier.verifyAndDecode("signed.payload")).thenReturn(payload("SUBSCRIBED", "Staging", BUNDLE, SIGNED_AT));

            assertFails(HttpStatus.FORBIDDEN, "error.subscription.webhook_unauthorized",
                    () -> subscriptionService.handleAppleWebhook("signed.payload"));
        }

        @Test
        @DisplayName("sandbox notification while sandbox is not allowed → acknowledged and ignored")
        void sandboxNotAllowed_ignored() {
            when(appleJwsVerifier.verifyAndDecode("signed.payload")).thenReturn(payload("SUBSCRIBED", "Sandbox", BUNDLE, SIGNED_AT));

            subscriptionService.handleAppleWebhook("signed.payload");

            verifyNoInteractions(subscriptionRS, authRS);
        }

        @Test
        @DisplayName("sandbox notification with APPLE_ALLOW_SANDBOX → processed")
        void sandboxAllowed_processed() {
            final SubscriptionService sandboxOk = newService(new IapSettings(BUNDLE, true, "m", "y", "", "", ""));
            when(appleJwsVerifier.verifyAndDecode("signed.payload")).thenReturn(payload("EXPIRED", "Sandbox", BUNDLE, SIGNED_AT));
            when(appleJwsVerifier.verifyAndDecode("signed.tx")).thenReturn(tx("Sandbox", BUNDLE, 0L));
            final SubscriptionDocument sub = existingSub(SubscriptionStatus.ACTIVE);
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));
            stubAuth();

            sandboxOk.handleAppleWebhook("signed.payload");

            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        }

        @Test
        @DisplayName("missing signedDate → 403 (cannot be ordered or deduplicated)")
        void missingSignedDate_forbidden() {
            when(appleJwsVerifier.verifyAndDecode("signed.payload")).thenReturn(payload("SUBSCRIBED", "Production", BUNDLE, null));
            when(appleJwsVerifier.verifyAndDecode("signed.tx")).thenReturn(tx("Production", BUNDLE, 0L));

            assertFails(HttpStatus.FORBIDDEN, "error.subscription.webhook_unauthorized",
                    () -> subscriptionService.handleAppleWebhook("signed.payload"));
        }

        @Test
        @DisplayName("TEST notification without data → acknowledged")
        void testNotification_ignored() {
            final ObjectNode test = mapper.createObjectNode().put("notificationType", "TEST");
            when(appleJwsVerifier.verifyAndDecode("signed.payload")).thenReturn(test);

            subscriptionService.handleAppleWebhook("signed.payload");

            verifyNoInteractions(subscriptionRS, authRS);
        }

        @Test
        @DisplayName("unknown subscription → acknowledged without changes")
        void unknownSubscription_ignored() {
            stubValidNotification("EXPIRED");
            when(subscriptionRS.findOptionalByOriginalTransactionId(TX_ID)).thenReturn(Optional.empty());

            subscriptionService.handleAppleWebhook("signed.payload");

            verify(subscriptionRS, never()).save(any());
            verifyNoInteractions(authRS);
        }

        @Test
        @DisplayName("no bundle id configured → 503 apple_not_configured, nothing verified")
        void notConfigured_serviceUnavailable() {
            final SubscriptionService unconfigured = newService(new IapSettings("", false, "", "", "", "", ""));

            assertFails(HttpStatus.SERVICE_UNAVAILABLE, "error.subscription.apple_not_configured",
                    () -> unconfigured.handleAppleWebhook("signed.payload"));

            verifyNoInteractions(appleJwsVerifier, subscriptionRS, authRS);
        }
    }

    // ─── Google webhook ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("handleGoogleWebhook")
    class HandleGoogleWebhook {

        private static final String AUTH = "Bearer push-token";
        private static final long EVENT_AT = 1_800_000_000_000L;

        private String message(final int type, final String token, final String packageName, final Long eventTime) {
            final String time = eventTime == null ? "" : ",\"eventTimeMillis\":\"" + eventTime + "\"";
            return Base64.getEncoder().encodeToString(("{\"version\":\"1.0\",\"packageName\":\"" + packageName + "\"" + time
                    + ",\"subscriptionNotification\":{\"notificationType\":" + type
                    + ",\"purchaseToken\":\"" + token + "\"}}").getBytes());
        }

        private SubscriptionDocument existingSub(final SubscriptionStatus status) {
            final SubscriptionDocument sub = SubscriptionDocument.builder().userId(USER_ID).status(status).build();
            when(subscriptionRS.findOptionalByPurchaseToken("tok")).thenReturn(Optional.of(sub));
            return sub;
        }

        private AuthDocument stubAuth(final boolean premium) {
            final AuthDocument auth = buildAuthDoc();
            auth.setPremium(premium);
            when(authRS.findById(USER_ID)).thenReturn(auth);
            when(authRS.save(auth)).thenReturn(auth);
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));
            return auth;
        }

        @Test
        @DisplayName("RENEWED → ACTIVE, premium on, event time recorded")
        void renewed_setsActiveAndPremium() {
            final SubscriptionDocument sub = existingSub(SubscriptionStatus.EXPIRED);
            final AuthDocument auth = stubAuth(false);

            subscriptionService.handleGoogleWebhook(AUTH, message(4, "tok", PACKAGE, EVENT_AT));

            verify(pubSubTokenVerifier).verify(AUTH);
            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(sub.getLastNotificationMs()).isEqualTo(EVENT_AT);
            assertThat(auth.isPremium()).isTrue();
        }

        @Test
        @DisplayName("EXPIRED → EXPIRED and premium off")
        void expired_setsExpired() {
            final SubscriptionDocument sub = existingSub(SubscriptionStatus.ACTIVE);
            final AuthDocument auth = stubAuth(true);

            subscriptionService.handleGoogleWebhook(AUTH, message(13, "tok", PACKAGE, EVENT_AT));

            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
            assertThat(auth.isPremium()).isFalse();
        }

        @Test
        @DisplayName("CANCELED → CANCELLED and premium off")
        void canceled_setsCancelled() {
            final SubscriptionDocument sub = existingSub(SubscriptionStatus.ACTIVE);
            final AuthDocument auth = stubAuth(true);

            subscriptionService.handleGoogleWebhook(AUTH, message(3, "tok", PACKAGE, EVENT_AT));

            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
            assertThat(auth.isPremium()).isFalse();
        }

        @Test
        @DisplayName("ON_HOLD → ON_HOLD and premium off")
        void onHold_setsOnHold() {
            final SubscriptionDocument sub = existingSub(SubscriptionStatus.ACTIVE);
            final AuthDocument auth = stubAuth(true);

            subscriptionService.handleGoogleWebhook(AUTH, message(5, "tok", PACKAGE, EVENT_AT));

            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ON_HOLD);
            assertThat(auth.isPremium()).isFalse();
        }

        @Test
        @DisplayName("unhandled notification type → nothing saved")
        void unhandledType_ignored() {
            existingSub(SubscriptionStatus.ACTIVE);

            subscriptionService.handleGoogleWebhook(AUTH, message(8, "tok", PACKAGE, EVENT_AT));

            verify(subscriptionRS, never()).save(any());
        }

        @Test
        @DisplayName("replay: event time not newer than the last applied one → ignored")
        void staleNotification_ignored() {
            final SubscriptionDocument sub = existingSub(SubscriptionStatus.ACTIVE);
            sub.setLastNotificationMs(EVENT_AT);

            subscriptionService.handleGoogleWebhook(AUTH, message(13, "tok", PACKAGE, EVENT_AT));

            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            verify(subscriptionRS, never()).save(any());
            verifyNoInteractions(authRS);
        }

        @Test
        @DisplayName("invalid push token → 403 and nothing is read or written")
        void invalidToken_forbidden() {
            org.mockito.Mockito.doThrow(BaseException.forbidden("error.subscription.webhook_unauthorized"))
                    .when(pubSubTokenVerifier).verify("Bearer forged");

            assertFails(HttpStatus.FORBIDDEN, "error.subscription.webhook_unauthorized",
                    () -> subscriptionService.handleGoogleWebhook("Bearer forged", message(13, "tok", PACKAGE, EVENT_AT)));

            verifyNoInteractions(subscriptionRS, authRS);
        }

        @Test
        @DisplayName("notification for another package → acknowledged and ignored")
        void otherPackage_ignored() {
            subscriptionService.handleGoogleWebhook(AUTH, message(13, "tok", "com.other.app", EVENT_AT));

            verifyNoInteractions(subscriptionRS, authRS);
        }

        @Test
        @DisplayName("no event time → 400, cannot be ordered or deduplicated")
        void missingEventTime_badRequest() {
            assertFails(HttpStatus.BAD_REQUEST, "error.subscription.verification_failed",
                    () -> subscriptionService.handleGoogleWebhook(AUTH, message(13, "tok", PACKAGE, null)));
        }

        @Test
        @DisplayName("unknown purchase token → acknowledged without changes")
        void unknownToken_ignored() {
            when(subscriptionRS.findOptionalByPurchaseToken("tok")).thenReturn(Optional.empty());

            subscriptionService.handleGoogleWebhook(AUTH, message(13, "tok", PACKAGE, EVENT_AT));

            verify(subscriptionRS, never()).save(any());
            verifyNoInteractions(authRS);
        }

        @Test
        @DisplayName("non-subscription notification → no DB call")
        void nonSubscription_ignored() {
            final String data = Base64.getEncoder().encodeToString(
                    ("{\"packageName\":\"" + PACKAGE + "\",\"oneTimeProductNotification\":{}}").getBytes());

            subscriptionService.handleGoogleWebhook(AUTH, data);

            verifyNoInteractions(subscriptionRS, authRS);
        }

        @Test
        @DisplayName("push without message data → acknowledged")
        void noData_ignored() {
            subscriptionService.handleGoogleWebhook(AUTH, null);

            verify(pubSubTokenVerifier).verify(AUTH);
            verifyNoInteractions(subscriptionRS, authRS);
        }

        @Test
        @DisplayName("invalid base64 → 400")
        void invalidBase64_badRequest() {
            assertFails(HttpStatus.BAD_REQUEST, "error.subscription.verification_failed",
                    () -> subscriptionService.handleGoogleWebhook(AUTH, "***not-base64***"));
        }

        @Test
        @DisplayName("no GOOGLE_PLAY_PACKAGE_NAME → 503 google_not_configured")
        void notConfigured_serviceUnavailable() {
            final SubscriptionService noPackage = newService(new IapSettings(BUNDLE, false, "m", "y", "", "aud", "push@example.iam.test"));

            assertFails(HttpStatus.SERVICE_UNAVAILABLE, "error.subscription.google_not_configured",
                    () -> noPackage.handleGoogleWebhook(AUTH, message(13, "tok", PACKAGE, EVENT_AT)));

            verifyNoInteractions(subscriptionRS, authRS);
        }
    }
}
