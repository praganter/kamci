package dev.batuhanyetgin.authservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AppleReceiptVerifier {

    private static final int APPLE_STATUS_OK = 0;
    private static final int APPLE_STATUS_SANDBOX = 21007;

    RestTemplate restTemplate = new RestTemplate();

    @Value("${apple.store.shared-secret}")
    private String sharedSecret;

    @Value("${apple.store.verify-url}")
    private String verifyUrl;

    @Value("${apple.store.sandbox-verify-url}")
    private String sandboxVerifyUrl;

    public JsonNode verify(final String receiptData) {
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
            final int status = result.path("status").asInt(-1);

            if (status == APPLE_STATUS_SANDBOX) {
                final ResponseEntity<JsonNode> sandboxResponse = restTemplate.postForEntity(sandboxVerifyUrl, entity, JsonNode.class);
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
        final JsonNode latestReceiptInfo = result.path("latest_receipt_info");
        if (!latestReceiptInfo.isArray() || latestReceiptInfo.isEmpty()) {
            throw BaseException.badRequest("error.subscription.verification_failed");
        }
        return latestReceiptInfo.get(latestReceiptInfo.size() - 1);
    }
}
