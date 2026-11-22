package dev.batuhanyetgin.authservice.service;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

/** The legacy verifyReceipt endpoint: production first, sandbox on status 21007. */
@Slf4j
@Service
public class LegacyAppleReceiptVerifier implements AppleReceiptVerifier {

    private static final int APPLE_STATUS_OK = 0;
    private static final int APPLE_STATUS_SANDBOX = 21007;

    RestTemplate restTemplate = IapHttp.restTemplate();

    private final IapSettings settings;

    @Value("${apple.store.shared-secret:${APPLE_SHARED_SECRET:}}")
    private String sharedSecret;

    @Value("${apple.store.verify-url:https://buy.itunes.apple.com/verifyReceipt}")
    private String verifyUrl;

    @Value("${apple.store.sandbox-verify-url:https://sandbox.itunes.apple.com/verifyReceipt}")
    private String sandboxVerifyUrl;

    public LegacyAppleReceiptVerifier(final IapSettings settings) {
        this.settings = settings;
    }

    @Override
    public boolean isConfigured() {
        return !sharedSecret.isBlank() && settings.isAppleConfigured();
    }

    @Override
    public JsonNode verify(final String receiptData) {
        if (!isConfigured()) {
            throw BaseException.serviceUnavailable("error.subscription.apple_not_configured");
        }

        final Map<String, String> body = new HashMap<>();
        body.put("receipt-data", receiptData);
        body.put("password", sharedSecret);
        body.put("exclude-old-transactions", "true");

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            final ResponseEntity<JsonNode> response = restTemplate.postForEntity(verifyUrl, entity, JsonNode.class);
            final JsonNode result = response.getBody();
            if (result != null && result.path("status").asInt(-1) == APPLE_STATUS_SANDBOX) {
                if (!settings.isAppleAllowSandbox()) {
                    log.warn("Sandbox receipt rejected (APPLE_ALLOW_SANDBOX is off)");
                    throw BaseException.badRequest("error.subscription.verification_failed");
                }
                final ResponseEntity<JsonNode> sandboxResponse =
                        restTemplate.postForEntity(sandboxVerifyUrl, entity, JsonNode.class);
                return extractReceiptInfo(sandboxResponse.getBody());
            }
            return extractReceiptInfo(result);
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Apple receipt verification failed: {}", e.getMessage());
            throw BaseException.badRequest("error.subscription.verification_failed");
        }
    }

    private JsonNode extractReceiptInfo(final JsonNode result) {
        if (result == null || result.path("status").asInt(-1) != APPLE_STATUS_OK) {
            throw BaseException.badRequest("error.subscription.verification_failed");
        }
        if (!settings.getAppleBundleId().equals(result.path("receipt").path("bundle_id").asString(null))) {
            log.warn("Apple receipt was issued for a different bundle id");
            throw BaseException.badRequest("error.subscription.verification_failed");
        }

        final JsonNode latestReceiptInfo = result.path("latest_receipt_info");
        if (!latestReceiptInfo.isArray() || latestReceiptInfo.isEmpty()) {
            throw BaseException.badRequest("error.subscription.verification_failed");
        }

        JsonNode newest = null;
        for (final JsonNode entry : latestReceiptInfo) {
            if (entry.hasNonNull("cancellation_date_ms")) {
                continue;
            }
            if (newest == null || entry.path("expires_date_ms").asLong(0) > newest.path("expires_date_ms").asLong(0)) {
                newest = entry;
            }
        }
        if (newest == null) {
            throw BaseException.badRequest("error.subscription.verification_failed");
        }
        return newest;
    }
}
