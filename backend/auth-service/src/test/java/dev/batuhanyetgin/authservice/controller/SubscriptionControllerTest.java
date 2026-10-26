package dev.batuhanyetgin.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock private SubscriptionService subscriptionService;

    private SubscriptionController controller;

    private static final String USER_ID    = "user123";
    private static final String ADMIN_KEY  = "secret-admin-key";
    private static final String RC_SECRET  = "rc-webhook-secret";

    @BeforeEach
    void setUp() {
        controller = new SubscriptionController(subscriptionService);
        ReflectionTestUtils.setField(controller, "adminSecretKey", ADMIN_KEY);
        ReflectionTestUtils.setField(controller, "revenueCatWebhookSecret", RC_SECRET);
    }

    private SubscriptionResponse buildResponse() {
        return SubscriptionResponse.builder()
                .plan(SubscriptionPlan.YEARLY)
                .status(SubscriptionStatus.ACTIVE)
                .build();
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

    // ─── revenueCatWebhook ────────────────────────────────────────────────────

    @Nested
    @DisplayName("revenueCatWebhook")
    class RevenueCatWebhook {

        private final ObjectMapper mapper = new ObjectMapper();

        private ObjectNode buildPayload() {
            final ObjectNode event = mapper.createObjectNode();
            event.put("type", "INITIAL_PURCHASE");
            event.put("app_user_id", USER_ID);
            final ObjectNode payload = mapper.createObjectNode();
            payload.set("event", event);
            return payload;
        }

        @Test
        @DisplayName("valid secret → delegates to service and returns 200")
        void validSecret_delegatesAndReturns200() {
            final ObjectNode payload = buildPayload();

            final ResponseEntity<Void> response = controller.revenueCatWebhook("Bearer " + RC_SECRET, payload);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(subscriptionService).handleRevenueCatWebhook(payload);
        }

        @Test
        @DisplayName("missing or wrong secret → throws FORBIDDEN, no service call")
        void invalidSecret_throwsForbidden() {
            final ObjectNode payload = buildPayload();

            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> controller.revenueCatWebhook("Bearer wrong-secret", payload))
                    .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);

            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> controller.revenueCatWebhook(null, payload))
                    .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);

            verifyNoInteractions(subscriptionService);
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
