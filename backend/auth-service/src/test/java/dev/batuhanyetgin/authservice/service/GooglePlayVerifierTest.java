package dev.batuhanyetgin.authservice.service;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import tools.jackson.databind.JsonNode;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Real RestTemplate with Jackson 3 conversion against a mock HTTP server. */
class GooglePlayVerifierTest {

    private static final String BASE =
            "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/";

    private GooglePlayVerifier verifier;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        verifier = new GooglePlayVerifier();
        server = MockRestServiceServer.bindTo(verifier.restTemplate).build();
    }

    private void withCredentials() {
        ReflectionTestUtils.setField(verifier, "credentials",
                GoogleCredentials.create(new AccessToken("test-token", null)));
    }

    @Test
    @DisplayName("no credentials → not configured, verification throws IllegalStateException")
    void noCredentials_notConfigured() {
        assertThat(verifier.isConfigured()).isFalse();

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> verifier.verifySubscription("com.example.app", "monthly", "token"));
    }

    @Test
    @DisplayName("GOOGLE_PLAY_CREDENTIALS unset → credentials stay null, no fallback to application default")
    void unsetPath_noFallback() {
        ReflectionTestUtils.setField(verifier, "credentialsPath", "");

        ReflectionTestUtils.invokeMethod(verifier, "loadCredentials");

        assertThat(verifier.isConfigured()).isFalse();
    }

    @Test
    @DisplayName("unreadable credentials file → stays not configured instead of failing startup")
    void unreadableFile_notConfigured() {
        ReflectionTestUtils.setField(verifier, "credentialsPath", "does-not-exist.json");

        ReflectionTestUtils.invokeMethod(verifier, "loadCredentials");

        assertThat(verifier.isConfigured()).isFalse();
    }

    @Test
    @DisplayName("valid purchase → orderId and expiryTimeMillis returned, bearer token sent")
    void validPurchase_returnsPurchaseInfo() throws Exception {
        withCredentials();
        server.expect(requestTo(BASE + "com.example.app/purchases/subscriptions/monthly/tokens/purchase-token"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withSuccess("{\"orderId\":\"GPA.ORDER-1\",\"expiryTimeMillis\":\"1234\",\"paymentState\":1}",
                        MediaType.APPLICATION_JSON));

        final JsonNode result = verifier.verifySubscription("com.example.app", "monthly", "purchase-token");

        assertThat(result.path("orderId").asString()).isEqualTo("GPA.ORDER-1");
        assertThat(result.path("expiryTimeMillis").asLong()).isEqualTo(1234L);
        server.verify();
    }

    @Test
    @DisplayName("every client-supplied value is encoded as a single path segment")
    void clientValuesAreEncoded() throws Exception {
        withCredentials();
        server.expect(requestTo(URI.create(BASE + "com.example.app/purchases/subscriptions/a%2Fb/tokens/..%2F..%2Fx%3Fy=1")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        verifier.verifySubscription("com.example.app", "a/b", "../../x?y=1");

        server.verify();
    }

    @Test
    @DisplayName("API answers 404 for an invalid token → exception")
    void invalidToken_throwsException() {
        withCredentials();
        server.expect(requestTo(BASE + "com.example.app/purchases/subscriptions/monthly/tokens/bad-token"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatException()
                .isThrownBy(() -> verifier.verifySubscription("com.example.app", "monthly", "bad-token"));
    }
}
