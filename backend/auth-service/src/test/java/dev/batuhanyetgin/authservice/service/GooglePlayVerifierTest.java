package dev.batuhanyetgin.authservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GooglePlayVerifierTest {

    @Mock private RestTemplate restTemplate;

    private GooglePlayVerifier verifier;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        verifier = new GooglePlayVerifier();
        verifier.restTemplate = restTemplate;
    }

    private ObjectNode buildPurchaseResponse(final String orderId, final long expiryTimeMillis) {
        final ObjectNode node = mapper.createObjectNode();
        node.put("orderId", orderId);
        node.put("expiryTimeMillis", expiryTimeMillis);
        node.put("paymentState", 1);
        return node;
    }

    @Nested
    @DisplayName("verifySubscription")
    class VerifySubscription {

        @Test
        @DisplayName("credentials not loaded → throws IllegalStateException")
        void noCredentials_throwsIllegalState() {
            ReflectionTestUtils.setField(verifier, "credentials", null);

            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(() -> verifier.verifySubscription("com.example.app", "monthly", "token"));
        }

        @Test
        @DisplayName("API returns valid purchase → orderId and expiryTimeMillis returned")
        void validPurchase_returnsPurchaseInfo() throws Exception {
            final long expiryMs = System.currentTimeMillis() + 86_400_000L;
            final ObjectNode purchaseResponse = buildPurchaseResponse("GPA.ORDER-1", expiryMs);

            final com.google.auth.oauth2.AccessToken accessToken =
                    new com.google.auth.oauth2.AccessToken("test-token", null);
            final com.google.auth.oauth2.GoogleCredentials mockCreds =
                    com.google.auth.oauth2.GoogleCredentials.create(accessToken);

            ReflectionTestUtils.setField(verifier, "credentials", mockCreds);

            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
                    .thenReturn(ResponseEntity.ok(purchaseResponse));

            final JsonNode result = verifier.verifySubscription("com.example.app", "monthly", "purchase-token");

            assertThat(result.path("orderId").asText()).isEqualTo("GPA.ORDER-1");
            assertThat(result.path("expiryTimeMillis").asLong()).isEqualTo(expiryMs);
        }

        @Test
        @DisplayName("API returns 404 (invalid token) → throws exception")
        void invalidToken_apiReturns404_throwsException() throws Exception {
            final com.google.auth.oauth2.AccessToken accessToken =
                    new com.google.auth.oauth2.AccessToken("test-token", null);
            final com.google.auth.oauth2.GoogleCredentials mockCreds =
                    com.google.auth.oauth2.GoogleCredentials.create(accessToken);

            ReflectionTestUtils.setField(verifier, "credentials", mockCreds);

            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
                    .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

            assertThatException()
                    .isThrownBy(() -> verifier.verifySubscription("com.example.app", "monthly", "bad-token"));
        }

        @Test
        @DisplayName("URL built correctly from packageName, subscriptionId, purchaseToken")
        void urlBuiltCorrectly() throws Exception {
            final com.google.auth.oauth2.AccessToken accessToken =
                    new com.google.auth.oauth2.AccessToken("test-token", null);
            final com.google.auth.oauth2.GoogleCredentials mockCreds =
                    com.google.auth.oauth2.GoogleCredentials.create(accessToken);

            ReflectionTestUtils.setField(verifier, "credentials", mockCreds);

            final String expectedUrl =
                    "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/com.example.app/purchases/subscriptions/yearly/tokens/tok123";

            when(restTemplate.exchange(eq(expectedUrl), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
                    .thenReturn(ResponseEntity.ok(buildPurchaseResponse("order1", 0L)));

            assertThatNoException()
                    .isThrownBy(() -> verifier.verifySubscription("com.example.app", "yearly", "tok123"));
        }
    }
}
