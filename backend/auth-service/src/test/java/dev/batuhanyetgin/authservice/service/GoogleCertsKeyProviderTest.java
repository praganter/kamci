package dev.batuhanyetgin.authservice.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import tools.jackson.databind.json.JsonMapper;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleCertsKeyProviderTest {

    private static final String CERTS_URL = "https://certs.example.test/oauth2/v3/certs";

    private static RSAPublicKey publicKey;

    private GoogleCertsKeyProvider provider;
    private MockRestServiceServer server;

    @BeforeAll
    static void generateKey() throws Exception {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        final KeyPair pair = generator.generateKeyPair();
        publicKey = (RSAPublicKey) pair.getPublic();
    }

    @BeforeEach
    void setUp() {
        provider = new GoogleCertsKeyProvider(new JsonMapper());
        ReflectionTestUtils.setField(provider, "certsUrl", CERTS_URL);
        server = MockRestServiceServer.bindTo(provider.restTemplate).build();
    }

    private static String b64(final java.math.BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = java.util.Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String jwks(final String kid) {
        return "{\"keys\":[{\"kty\":\"RSA\",\"alg\":\"RS256\",\"use\":\"sig\",\"kid\":\"" + kid + "\","
                + "\"n\":\"" + b64(publicKey.getModulus()) + "\",\"e\":\"" + b64(publicKey.getPublicExponent()) + "\"},"
                + "{\"kty\":\"EC\",\"kid\":\"ignored\",\"x\":\"a\",\"y\":\"b\"}]}";
    }

    @Test
    @DisplayName("builds the RSA public key from the published JWKS")
    void parsesJwks() {
        server.expect(requestTo(CERTS_URL)).andRespond(withSuccess(jwks("k1"), MediaType.APPLICATION_JSON));

        final RSAPublicKey key = (RSAPublicKey) provider.keyFor("k1");

        assertThat(key).isNotNull();
        assertThat(key.getModulus()).isEqualTo(publicKey.getModulus());
        assertThat(key.getPublicExponent()).isEqualTo(publicKey.getPublicExponent());
        server.verify();
    }

    @Test
    @DisplayName("keys are cached: a second lookup does not fetch again")
    void cached() {
        server.expect(ExpectedCount.once(), requestTo(CERTS_URL)).andRespond(withSuccess(jwks("k1"), MediaType.APPLICATION_JSON));

        assertThat(provider.keyFor("k1")).isNotNull();
        assertThat(provider.keyFor("k1")).isNotNull();

        server.verify();
    }

    @Test
    @DisplayName("unknown kid → null, and unknown kids cannot trigger more than one fetch a minute")
    void unknownKid_rateLimitedRefresh() {
        server.expect(ExpectedCount.once(), requestTo(CERTS_URL)).andRespond(withSuccess(jwks("k1"), MediaType.APPLICATION_JSON));

        assertThat(provider.keyFor("nope")).isNull();
        assertThat(provider.keyFor("still-nope")).isNull();
        assertThat(provider.keyFor(null)).isNull();

        server.verify();
    }

    @Test
    @DisplayName("an unknown kid after the minimum interval re-fetches and finds a rotated key")
    void rotatedKey_foundAfterRefresh() {
        server.expect(ExpectedCount.once(), requestTo(CERTS_URL)).andRespond(withSuccess(jwks("old"), MediaType.APPLICATION_JSON));
        assertThat(provider.keyFor("new")).isNull();
        server.verify();

        ReflectionTestUtils.setField(provider, "fetchedAt", Instant.now().minusSeconds(120));
        server = MockRestServiceServer.bindTo(provider.restTemplate).build();
        server.expect(ExpectedCount.once(), requestTo(CERTS_URL)).andRespond(withSuccess(jwks("new"), MediaType.APPLICATION_JSON));

        assertThat(provider.keyFor("new")).isNotNull();
        server.verify();
    }

    @Test
    @DisplayName("failed refresh keeps the keys already cached")
    void failedRefresh_keepsOldKeys() {
        server.expect(ExpectedCount.once(), requestTo(CERTS_URL)).andRespond(withSuccess(jwks("k1"), MediaType.APPLICATION_JSON));
        assertThat(provider.keyFor("k1")).isNotNull();
        server.verify();

        ReflectionTestUtils.setField(provider, "fetchedAt", Instant.now().minusSeconds(7_200));
        server = MockRestServiceServer.bindTo(provider.restTemplate).build();
        server.expect(ExpectedCount.once(), requestTo(CERTS_URL)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThat(provider.keyFor("k1")).isNotNull();
        server.verify();
    }

    @Test
    @DisplayName("garbage response → null, no exception")
    void garbageResponse_returnsNull() {
        server.expect(requestTo(CERTS_URL)).andRespond(withSuccess("not json", MediaType.APPLICATION_JSON));

        assertThat(provider.keyFor("k1")).isNull();
    }
}
