package dev.batuhanyetgin.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/** Google's published RSA signing keys (JWKS), cached for an hour and re-fetched for unknown key ids. */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCertsKeyProvider implements PubSubKeyProvider {

    private static final Duration MAX_AGE = Duration.ofHours(1);
    private static final Duration MIN_REFRESH_INTERVAL = Duration.ofMinutes(1);

    RestTemplate restTemplate = IapHttp.restTemplate();

    private final JsonMapper jsonMapper;

    @Value("${google.pubsub.certs-url:https://www.googleapis.com/oauth2/v3/certs}")
    private String certsUrl;

    private volatile Map<String, PublicKey> keys = Map.of();
    private volatile Instant fetchedAt = Instant.EPOCH;

    @Override
    public PublicKey keyFor(final String keyId) {
        if (keyId == null) {
            return null;
        }
        final PublicKey cached = keys.get(keyId);
        if (cached != null && Duration.between(fetchedAt, Instant.now()).compareTo(MAX_AGE) < 0) {
            return cached;
        }
        refresh();
        return keys.get(keyId);
    }

    private synchronized void refresh() {
        // An unknown kid must not let a caller make us hit Google on every request.
        if (Duration.between(fetchedAt, Instant.now()).compareTo(MIN_REFRESH_INTERVAL) < 0) {
            return;
        }
        fetchedAt = Instant.now();
        try {
            final String body = restTemplate.getForObject(certsUrl, String.class);
            final Map<String, PublicKey> fresh = new HashMap<>();
            final KeyFactory rsa = KeyFactory.getInstance("RSA");
            for (final JsonNode jwk : jsonMapper.readTree(body).path("keys")) {
                if (!"RSA".equals(jwk.path("kty").asString(null)) || !jwk.hasNonNull("kid")) {
                    continue;
                }
                final BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.path("n").asString()));
                final BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.path("e").asString()));
                fresh.put(jwk.path("kid").asString(), rsa.generatePublic(new RSAPublicKeySpec(modulus, exponent)));
            }
            if (!fresh.isEmpty()) {
                keys = fresh;
            }
        } catch (Exception e) {
            log.warn("Could not refresh Google signing keys: {}", e.getMessage());
        }
    }
}
