package dev.batuhanyetgin.authservice.service;

import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;
import tools.jackson.databind.JsonNode;

import java.io.FileInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Checks a subscription purchase token with the Google Play Developer API.
 *
 * <p>The service account comes from {@code GOOGLE_PLAY_CREDENTIALS} (a key file path). It is not
 * read from {@code GOOGLE_APPLICATION_CREDENTIALS}, which the deployment points at the Firebase
 * (push notification) account that has no Play Console access.
 */
@Slf4j
@Service
public class GooglePlayVerifier {

    private static final String SCOPE = "https://www.googleapis.com/auth/androidpublisher";
    private static final String API_URL = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/%s"
            + "/purchases/subscriptions/%s/tokens/%s";

    RestTemplate restTemplate = IapHttp.restTemplate();

    @Value("${GOOGLE_PLAY_CREDENTIALS:}")
    private String credentialsPath;

    private GoogleCredentials credentials;

    @PostConstruct
    void loadCredentials() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.info("GOOGLE_PLAY_CREDENTIALS is not set, Android purchase verification is disabled");
            return;
        }
        try (final var is = new FileInputStream(credentialsPath)) {
            credentials = GoogleCredentials.fromStream(is)
                    .createScoped(Collections.singletonList(SCOPE));
            log.info("Google Play credentials loaded");
        } catch (Exception e) {
            log.warn("Google Play credentials could not be loaded, Android purchase verification is disabled: {}",
                    e.getMessage());
            credentials = null;
        }
    }

    public boolean isConfigured() {
        return credentials != null;
    }

    public JsonNode verifySubscription(final String packageName,
                                       final String subscriptionId,
                                       final String purchaseToken) throws Exception {
        if (credentials == null) {
            throw new IllegalStateException("Google Play credentials not configured");
        }

        credentials.refreshIfExpired();
        final String accessToken = credentials.getAccessToken().getTokenValue();

        final HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        // Client-supplied values go into path segments, so each one is encoded on its own.
        final URI uri = URI.create(String.format(API_URL, segment(packageName), segment(subscriptionId), segment(purchaseToken)));
        final ResponseEntity<JsonNode> response = restTemplate.exchange(
                uri, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        return response.getBody();
    }

    private static String segment(final String value) {
        return UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
    }
}
