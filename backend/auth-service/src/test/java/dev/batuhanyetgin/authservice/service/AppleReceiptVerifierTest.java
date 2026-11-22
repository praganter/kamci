package dev.batuhanyetgin.authservice.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppleReceiptVerifierTest {

    @Mock private RestTemplate restTemplate;

    private AppleReceiptVerifier verifier;

    private final JsonMapper mapper = new JsonMapper();

    private static final String VERIFY_URL   = "https://buy.itunes.apple.com/verifyReceipt";
    private static final String SANDBOX_URL  = "https://sandbox.itunes.apple.com/verifyReceipt";

    @BeforeEach
    void setUp() {
        verifier = new AppleReceiptVerifier();
        verifier.restTemplate = restTemplate;
        ReflectionTestUtils.setField(verifier, "sharedSecret",  "test-secret");
        ReflectionTestUtils.setField(verifier, "verifyUrl",     VERIFY_URL);
        ReflectionTestUtils.setField(verifier, "sandboxVerifyUrl", SANDBOX_URL);
    }

    private ObjectNode successBody(final String txId) {
        final ObjectNode receiptInfo = mapper.createObjectNode();
        receiptInfo.put("original_transaction_id", txId);
        receiptInfo.put("expires_date_ms", System.currentTimeMillis() + 86_400_000L);

        final ArrayNode latestReceiptInfo = mapper.createArrayNode();
        latestReceiptInfo.add(receiptInfo);

        final ObjectNode body = mapper.createObjectNode();
        body.put("status", 0);
        body.set("latest_receipt_info", latestReceiptInfo);
        return body;
    }

    @Nested
    @DisplayName("verify")
    class Verify {

        @Test
        @DisplayName("status 0 → returns last receipt info")
        void statusOk_returnsReceiptInfo() {
            final ObjectNode body = successBody("TX_PROD_1");
            when(restTemplate.postForEntity(eq(VERIFY_URL), any(), eq(JsonNode.class)))
                    .thenReturn(ResponseEntity.ok(body));

            final JsonNode result = verifier.verify("receipt-data");

            assertThat(result.path("original_transaction_id").asString()).isEqualTo("TX_PROD_1");
        }

        @Test
        @DisplayName("status 21007 (sandbox) → retries against sandbox URL")
        void statusSandbox_retriesWithSandboxUrl() {
            final ObjectNode sandboxBody  = successBody("TX_SANDBOX_1");

            final ObjectNode prodResponse = mapper.createObjectNode();
            prodResponse.put("status", 21007);

            when(restTemplate.postForEntity(eq(VERIFY_URL), any(), eq(JsonNode.class)))
                    .thenReturn(ResponseEntity.ok(prodResponse));
            when(restTemplate.postForEntity(eq(SANDBOX_URL), any(), eq(JsonNode.class)))
                    .thenReturn(ResponseEntity.ok(sandboxBody));

            final JsonNode result = verifier.verify("sandbox-receipt");

            assertThat(result.path("original_transaction_id").asString()).isEqualTo("TX_SANDBOX_1");
        }

        @Test
        @DisplayName("non-zero status → throws BAD_REQUEST")
        void nonZeroStatus_throwsBadRequest() {
            final ObjectNode body = mapper.createObjectNode();
            body.put("status", 21002);

            when(restTemplate.postForEntity(eq(VERIFY_URL), any(), eq(JsonNode.class)))
                    .thenReturn(ResponseEntity.ok(body));

            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> verifier.verify("bad-receipt"))
                    .matches(e -> e.getStatus() == HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("empty latest_receipt_info array → throws BAD_REQUEST")
        void emptyReceiptInfoArray_throwsBadRequest() {
            final ObjectNode body = mapper.createObjectNode();
            body.put("status", 0);
            body.set("latest_receipt_info", mapper.createArrayNode());

            when(restTemplate.postForEntity(eq(VERIFY_URL), any(), eq(JsonNode.class)))
                    .thenReturn(ResponseEntity.ok(body));

            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> verifier.verify("empty-receipt"))
                    .matches(e -> e.getStatus() == HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("HTTP call throws exception → throws BAD_REQUEST")
        void httpCallFails_throwsBadRequest() {
            when(restTemplate.postForEntity(eq(VERIFY_URL), any(), eq(JsonNode.class)))
                    .thenThrow(new RuntimeException("Connection refused"));

            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> verifier.verify("any-receipt"))
                    .matches(e -> e.getStatus() == HttpStatus.BAD_REQUEST);
        }
    }
}
