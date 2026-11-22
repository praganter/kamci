package dev.batuhanyetgin.authservice.controller;

import dev.batuhanyetgin.authservice.dto.request.VerifyReceiptRequest;
import dev.batuhanyetgin.authservice.dto.response.SubscriptionResponse;
import dev.batuhanyetgin.authservice.enums.SubscriptionPlan;
import dev.batuhanyetgin.authservice.enums.SubscriptionStatus;
import dev.batuhanyetgin.authservice.service.SubscriptionService;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock private SubscriptionService subscriptionService;

    private SubscriptionController controller;

    private static final String USER_ID    = "user123";
    private static final String ADMIN_KEY  = "secret-admin-key";

    @BeforeEach
    void setUp() {
        controller = new SubscriptionController(subscriptionService);
        ReflectionTestUtils.setField(controller, "adminSecretKey", ADMIN_KEY);
    }

    private SubscriptionResponse buildResponse() {
        return SubscriptionResponse.builder()
                .plan(SubscriptionPlan.YEARLY)
                .status(SubscriptionStatus.ACTIVE)
                .build();
    }

    // ─── verify ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("verify")
    class Verify {

        @Test
        @DisplayName("delegates to service and returns 200")
        void delegatesAndReturns200() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(USER_ID);

                final VerifyReceiptRequest req = VerifyReceiptRequest.builder()
                        .platform("IOS")
                        .plan("YEARLY")
                        .receiptData("receipt-data")
                        .productId("product1")
                        .build();
                when(subscriptionService.verifyReceipt(USER_ID, req)).thenReturn(buildResponse());

                final ResponseEntity<SubscriptionResponse> response = controller.verify(req);

                assertThat(response.getStatusCode().value()).isEqualTo(200);
                assertThat(response.getBody().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
                verify(subscriptionService).verifyReceipt(USER_ID, req);
            }
        }
    }

    // ─── status ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("status")
    class Status {

        @Test
        @DisplayName("delegates to service and returns 200")
        void delegatesAndReturns200() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(USER_ID);

                when(subscriptionService.getStatus(USER_ID)).thenReturn(buildResponse());

                final ResponseEntity<SubscriptionResponse> response = controller.status();

                assertThat(response.getStatusCode().value()).isEqualTo(200);
                assertThat(response.getBody().getPlan()).isEqualTo(SubscriptionPlan.YEARLY);
                verify(subscriptionService).getStatus(USER_ID);
            }
        }
    }

    // ─── appleWebhook ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("appleWebhook")
    class AppleWebhook {

        @Test
        @DisplayName("payload with signedPayload → delegates to service and returns 200")
        void payloadPresent_delegatesAndReturns200() {
            final Map<String, String> payload = Map.of("signedPayload", "signed.jwt.token");

            final ResponseEntity<Void> response = controller.appleWebhook(payload);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(subscriptionService).handleAppleWebhook("signed.jwt.token");
        }

        @Test
        @DisplayName("payload without signedPayload → 400, no service call")
        void noSignedPayload_badRequest() {
            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> controller.appleWebhook(Map.of()))
                    .matches(e -> e.getStatus() == HttpStatus.BAD_REQUEST);
            verifyNoInteractions(subscriptionService);
        }
    }

    // ─── googleWebhook ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("googleWebhook")
    class GoogleWebhook {

        @Test
        @DisplayName("passes the Authorization header and message data to the service, returns 200")
        void validMessage_delegatesAndReturns200() {
            final Map<String, Object> payload = Map.of("message", Map.of("data", "base64encodedData"));

            final ResponseEntity<Void> response = controller.googleWebhook("Bearer push-token", payload);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(subscriptionService).handleGoogleWebhook("Bearer push-token", "base64encodedData");
        }

        @Test
        @DisplayName("no message key → the service is still called (it authenticates first), with null data")
        void noMessage_stillAuthenticated() {
            controller.googleWebhook("Bearer push-token", Map.of());

            verify(subscriptionService).handleGoogleWebhook("Bearer push-token", null);
        }

        @Test
        @DisplayName("malformed message (not a map) → null data, never a ClassCastException")
        void malformedMessage_nullData() {
            controller.googleWebhook(null, Map.of("message", "not-a-map"));

            verify(subscriptionService).handleGoogleWebhook(null, null);
        }

        @Test
        @DisplayName("service rejects the push token → 403 propagates")
        void forbidden_propagates() {
            doThrow(BaseException.forbidden("error.subscription.webhook_unauthorized"))
                    .when(subscriptionService).handleGoogleWebhook(null, null);

            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> controller.googleWebhook(null, Map.of()))
                    .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);
        }
    }

    // ─── grantPremium ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("grantPremium")
    class GrantPremium {

        @Test
        @DisplayName("valid admin key → delegates to service and returns 200")
        void validAdminKey_delegatesAndReturns200() {
            final ResponseEntity<Void> response = controller.grantPremium(ADMIN_KEY, USER_ID);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(subscriptionService).grantPremium(USER_ID);
        }

        @Test
        @DisplayName("invalid admin key → throws FORBIDDEN")
        void invalidAdminKey_throwsForbidden() {
            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> controller.grantPremium("wrong-key", USER_ID))
                    .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);

            verifyNoInteractions(subscriptionService);
        }
    }

    // ─── revokePremium ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("revokePremium")
    class RevokePremium {

        @Test
        @DisplayName("valid admin key → delegates to service and returns 200")
        void validAdminKey_delegatesAndReturns200() {
            final ResponseEntity<Void> response = controller.revokePremium(ADMIN_KEY, USER_ID);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(subscriptionService).revokePremium(USER_ID);
        }

        @Test
        @DisplayName("invalid admin key → throws FORBIDDEN")
        void invalidAdminKey_throwsForbidden() {
            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> controller.revokePremium("bad-key", USER_ID))
                    .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);

            verifyNoInteractions(subscriptionService);
        }
    }
}
