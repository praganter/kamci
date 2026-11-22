package dev.batuhanyetgin.authservice.service;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Runs the real RestTemplate with Jackson 3 conversion against a mock HTTP server. */
class LegacyAppleReceiptVerifierTest {

    private static final String VERIFY_URL = "https://verify.example.test/verifyReceipt";
    private static final String SANDBOX_URL = "https://sandbox.example.test/verifyReceipt";
    private static final String BUNDLE = "com.example.kamci";

    private LegacyAppleReceiptVerifier verifier;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        verifier = verifierFor(new IapSettings(BUNDLE, false, "m", "y"), "test-secret");
    }

    private LegacyAppleReceiptVerifier verifierFor(final IapSettings settings, final String secret) {
        final LegacyAppleReceiptVerifier v = new LegacyAppleReceiptVerifier(settings);
        ReflectionTestUtils.setField(v, "sharedSecret", secret);
        ReflectionTestUtils.setField(v, "verifyUrl", VERIFY_URL);
        ReflectionTestUtils.setField(v, "sandboxVerifyUrl", SANDBOX_URL);
        server = MockRestServiceServer.bindTo(v.restTemplate).build();
        return v;
    }

    private static String receiptBody(final int status, final String bundle, final String latestReceiptInfo) {
        return "{\"status\":" + status + ",\"receipt\":{\"bundle_id\":\"" + bundle + "\"},"
                + "\"latest_receipt_info\":" + latestReceiptInfo + "}";
    }

    private static void assertBadRequest(final Runnable call) {
        assertThatExceptionOfType(BaseException.class)
                .isThrownBy(call::run)
                .matches(e -> e.getStatus() == HttpStatus.BAD_REQUEST)
                .withMessage("error.subscription.verification_failed");
    }

    @Test
    @DisplayName("sends receipt-data, the shared secret and exclude-old-transactions")
    void sendsExpectedRequest() {
        server.expect(requestTo(VERIFY_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.receipt-data").value("RECEIPT"))
                .andExpect(jsonPath("$.password").value("test-secret"))
                .andExpect(jsonPath("$.exclude-old-transactions").value("true"))
                .andRespond(withSuccess(receiptBody(0, BUNDLE,
                        "[{\"product_id\":\"p\",\"original_transaction_id\":\"T1\",\"expires_date_ms\":\"100\"}]"),
                        MediaType.APPLICATION_JSON));

        final JsonNode info = verifier.verify("RECEIPT");

        assertThat(info.path("original_transaction_id").asString()).isEqualTo("T1");
        server.verify();
    }

    @Test
    @DisplayName("picks the entry with the latest expiry and skips cancelled ones")
    void picksNewestNonCancelled() {
        final String entries = "["
                + "{\"product_id\":\"p\",\"original_transaction_id\":\"OLD\",\"expires_date_ms\":\"100\"},"
                + "{\"product_id\":\"p\",\"original_transaction_id\":\"NEW\",\"expires_date_ms\":\"300\"},"
                + "{\"product_id\":\"p\",\"original_transaction_id\":\"REFUNDED\",\"expires_date_ms\":\"900\",\"cancellation_date_ms\":\"500\"}]";
        server.expect(requestTo(VERIFY_URL)).andRespond(withSuccess(receiptBody(0, BUNDLE, entries), MediaType.APPLICATION_JSON));

        assertThat(verifier.verify("R").path("original_transaction_id").asString()).isEqualTo("NEW");
    }

    @Test
    @DisplayName("every entry cancelled → 400")
    void allCancelled_badRequest() {
        server.expect(requestTo(VERIFY_URL)).andRespond(withSuccess(receiptBody(0, BUNDLE,
                "[{\"original_transaction_id\":\"X\",\"expires_date_ms\":\"9\",\"cancellation_date_ms\":\"5\"}]"),
                MediaType.APPLICATION_JSON));

        assertBadRequest(() -> verifier.verify("R"));
    }

    @Test
    @DisplayName("receipt issued for another bundle id → 400")
    void otherBundle_badRequest() {
        server.expect(requestTo(VERIFY_URL)).andRespond(withSuccess(receiptBody(0, "com.other.app",
                "[{\"original_transaction_id\":\"X\",\"expires_date_ms\":\"9\"}]"), MediaType.APPLICATION_JSON));

        assertBadRequest(() -> verifier.verify("R"));
    }

    @Test
    @DisplayName("Apple status other than 0 → 400")
    void nonZeroStatus_badRequest() {
        server.expect(requestTo(VERIFY_URL)).andRespond(withSuccess("{\"status\":21003}", MediaType.APPLICATION_JSON));

        assertBadRequest(() -> verifier.verify("R"));
    }

    @Test
    @DisplayName("no receipt entries → 400")
    void emptyLatestReceiptInfo_badRequest() {
        server.expect(requestTo(VERIFY_URL)).andRespond(withSuccess(receiptBody(0, BUNDLE, "[]"), MediaType.APPLICATION_JSON));

        assertBadRequest(() -> verifier.verify("R"));
    }

    @Test
    @DisplayName("Apple unreachable → 400, not a raw exception")
    void networkError_badRequest() {
        server.expect(requestTo(VERIFY_URL)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertBadRequest(() -> verifier.verify("R"));
    }

    @Test
    @DisplayName("status 21007 with sandbox allowed → retried against the sandbox endpoint")
    void sandboxReceipt_allowed_fallsBack() {
        verifier = verifierFor(new IapSettings(BUNDLE, true, "m", "y"), "test-secret");
        server.expect(requestTo(VERIFY_URL)).andRespond(withSuccess("{\"status\":21007}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(SANDBOX_URL)).andRespond(withSuccess(receiptBody(0, BUNDLE,
                "[{\"original_transaction_id\":\"SBX\",\"expires_date_ms\":\"9\"}]"), MediaType.APPLICATION_JSON));

        assertThat(verifier.verify("R").path("original_transaction_id").asString()).isEqualTo("SBX");
        server.verify();
    }

    @Test
    @DisplayName("status 21007 with sandbox not allowed → 400 and the sandbox endpoint is never called")
    void sandboxReceipt_notAllowed_rejected() {
        server.expect(requestTo(VERIFY_URL)).andRespond(withSuccess("{\"status\":21007}", MediaType.APPLICATION_JSON));
        server.expect(never(), requestTo(SANDBOX_URL));

        assertBadRequest(() -> verifier.verify("R"));
        server.verify();
    }

    @Test
    @DisplayName("no shared secret → not configured, 503 and no request sent")
    void noSecret_notConfigured() {
        verifier = verifierFor(new IapSettings(BUNDLE, false, "m", "y"), "");
        server.expect(never(), content().string(""));

        assertThat(verifier.isConfigured()).isFalse();
        assertThatExceptionOfType(BaseException.class)
                .isThrownBy(() -> verifier.verify("R"))
                .matches(e -> e.getStatus() == HttpStatus.SERVICE_UNAVAILABLE)
                .withMessage("error.subscription.apple_not_configured");
    }

    @Test
    @DisplayName("no bundle id → not configured")
    void noBundle_notConfigured() {
        assertThat(verifierFor(new IapSettings("", false, "m", "y"), "test-secret").isConfigured()).isFalse();
    }
}
