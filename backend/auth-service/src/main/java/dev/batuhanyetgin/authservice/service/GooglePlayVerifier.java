package dev.batuhanyetgin.authservice.service;

import com.fasterxml.jackson.databind.JsonNode;
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

import java.io.FileInputStream;
import java.util.Collections;

@Slf4j
@Service
public class GooglePlayVerifier {

    private static final String SCOPE = "https://www.googleapis.com/auth/androidpublisher";
    private static final String API_URL =
            "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/%s/purchases/subscriptions/%s/tokens/%s";

    RestTemplate restTemplate = new RestTemplate();

    @Value("${GOOGLE_APPLICATION_CREDENTIALS:}")
    private String credentialsPath;

    private GoogleCredentials credentials;

    @PostConstruct
    void loadCredentials() {
        try {
            if (credentialsPath != null && !credentialsPath.isBlank()) {
                try (final var is = new FileInputStream(credentialsPath)) {
                    credentials = GoogleCredentials.fromStream(is)
                            .createScoped(Collections.singletonList(SCOPE));
                }
                log.info("Google Play credentials loaded from: {}", credentialsPath);
            } else {
                credentials = GoogleCredentials.getApplicationDefault()
                        .createScoped(Collections.singletonList(SCOPE));
                log.info("Google Play credentials loaded from application default");
            }
        } catch (Exception e) {
            log.warn("Google Play credentials not configured — Android subscriptions will not work: {}", e.getMessage());
            credentials = null;
        }
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

        final String url = String.format(API_URL, packageName, subscriptionId, purchaseToken);
        final ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

        return response.getBody();
    }
}
