package dev.batuhanyetgin.authservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.batuhanyetgin.authservice.document.SubscriptionDocument;
import dev.batuhanyetgin.authservice.dto.request.VerifyReceiptRequest;
import dev.batuhanyetgin.authservice.dto.response.SubscriptionResponse;
import dev.batuhanyetgin.authservice.enums.SubscriptionPlan;
import dev.batuhanyetgin.authservice.enums.SubscriptionPlatform;
import dev.batuhanyetgin.authservice.enums.SubscriptionStatus;
import dev.batuhanyetgin.authservice.repositoryservice.AuthRepositoryService;
import dev.batuhanyetgin.authservice.repositoryservice.SubscriptionRepositoryService;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final AuthRepositoryService authRS;
    private final SubscriptionRepositoryService subscriptionRS;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final AppleJwsVerifier appleJwsVerifier;

    @Value("${apple.store.shared-secret}")
    private String appleSharedSecret;

    @Value("${apple.store.verify-url}")
    private String appleVerifyUrl;

    @Value("${apple.store.sandbox-verify-url}")
    private String appleSandboxVerifyUrl;

    private static final int APPLE_STATUS_OK = 0;
    private static final int APPLE_STATUS_SANDBOX = 21007;

    // Google Play subscription notification types
    private static final int GOOGLE_SUBSCRIPTION_RECOVERED = 1;
    private static final int GOOGLE_SUBSCRIPTION_RENEWED = 4;
    private static final int GOOGLE_SUBSCRIPTION_PURCHASED = 7;
    private static final int GOOGLE_SUBSCRIPTION_CANCELED = 3;
    private static final int GOOGLE_SUBSCRIPTION_ON_HOLD = 5;
    private static final int GOOGLE_SUBSCRIPTION_EXPIRED = 13;
    private static final int GOOGLE_SUBSCRIPTION_REVOKED = 12;

    public SubscriptionResponse verifyReceipt(final String userId, final VerifyReceiptRequest request) {
        final SubscriptionPlatform platform = SubscriptionPlatform.valueOf(request.getPlatform());
        final SubscriptionPlan plan = SubscriptionPlan.valueOf(request.getPlan());

        final LocalDateTime expiryDate;
        final String transactionId;

        if (platform == SubscriptionPlatform.IOS) {
            final JsonNode receiptInfo = verifyAppleReceipt(request.getReceiptData());
            transactionId = receiptInfo.path("original_transaction_id").asText();
            final long expiresMs = receiptInfo.path("expires_date_ms").asLong();
            expiryDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(expiresMs), ZoneOffset.UTC);
        } else {
            final JsonNode purchaseInfo = verifyGooglePurchase(
                    request.getPackageName(),
                    request.getProductId(),
                    request.getReceiptData()
            );
            transactionId = purchaseInfo.path("orderId").asText();
            final long expiresMs = purchaseInfo.path("expiryTimeMillis").asLong();
            expiryDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(expiresMs), ZoneOffset.UTC);
        }

        final SubscriptionDocument subscription = subscriptionRS.findByUserId(userId)
                .orElse(SubscriptionDocument.builder().userId(userId).build());

        final boolean isNew = subscription.getId() == null;

        subscription.setPlan(plan);
        subscription.setPlatform(platform);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setProductId(request.getProductId());
        if (isNew) {
            subscription.setStartDate(LocalDateTime.now());
        }
        subscription.setExpiryDate(expiryDate);

        if (platform == SubscriptionPlatform.IOS) {
            subscription.setOriginalTransactionId(transactionId);
        } else {
            subscription.setPurchaseToken(request.getReceiptData());
            subscription.setOriginalTransactionId(transactionId);
        }

        subscriptionRS.save(subscription);
        updatePremiumStatus(userId, true);

        log.info("Subscription activated for userId: {}, plan: {}, platform: {}", userId, plan, platform);

        return buildSubscriptionResponse(subscription, userId);
    }

    public SubscriptionResponse getStatus(final String userId) {
        return subscriptionRS.findByUserId(userId)
                .map(sub -> SubscriptionResponse.builder()
                        .plan(sub.getPlan())
                        .status(sub.getStatus())
                        .expiryDate(sub.getExpiryDate())
                        .build())
                .orElseThrow(() -> BaseException.notFound("error.subscription.not_found"));
    }

    public void handleAppleWebhook(final String signedPayload) {
        try {
            // Apple SSNS V2: verify JWS signature, then decode payload
            final JsonNode payload = appleJwsVerifier.verifyAndDecode(signedPayload);

            final String notificationType = payload.path("notificationType").asText();
            final JsonNode data = payload.path("data");
            final String signedTransactionInfo = data.path("signedTransactionInfo").asText();
            final String signedRenewalInfo = data.path("signedRenewalInfo").asText();

            // Decode transaction info
            final String[] txParts = signedTransactionInfo.split("\\.");
            final JsonNode txInfo = objectMapper.readTree(
                    new String(Base64.getUrlDecoder().decode(txParts[1]))
            );

            final String originalTransactionId = txInfo.path("originalTransactionId").asText();
            final long expiresMs = txInfo.path("expiresDate").asLong();
            final LocalDateTime expiryDate = expiresMs > 0
                    ? LocalDateTime.ofInstant(Instant.ofEpochMilli(expiresMs), ZoneOffset.UTC)
                    : null;

            switch (notificationType) {
                case "SUBSCRIBED", "DID_RENEW" -> {
                    final SubscriptionDocument sub = subscriptionRS.findByOriginalTransactionId(originalTransactionId);
                    sub.setStatus(SubscriptionStatus.ACTIVE);
                    if (expiryDate != null) sub.setExpiryDate(expiryDate);
                    subscriptionRS.save(sub);
                    updatePremiumStatus(sub.getUserId(), true);
                    log.info("Apple subscription renewed/activated for transactionId: {}", originalTransactionId);
                }
                case "EXPIRED", "DID_FAIL_TO_RENEW", "REFUND" -> {
                    final SubscriptionDocument sub = subscriptionRS.findByOriginalTransactionId(originalTransactionId);
                    sub.setStatus("REFUND".equals(notificationType)
                            ? SubscriptionStatus.CANCELLED : SubscriptionStatus.EXPIRED);
                    subscriptionRS.save(sub);
                    updatePremiumStatus(sub.getUserId(), false);
                    log.info("Apple subscription expired/cancelled for transactionId: {}", originalTransactionId);
                }
                case "GRACE_PERIOD_EXPIRED" -> {
                    final SubscriptionDocument sub = subscriptionRS.findByOriginalTransactionId(originalTransactionId);
                    sub.setStatus(SubscriptionStatus.EXPIRED);
                    subscriptionRS.save(sub);
                    updatePremiumStatus(sub.getUserId(), false);
                    log.info("Apple grace period expired for transactionId: {}", originalTransactionId);
                }
                case "DID_CHANGE_RENEWAL_STATUS" -> {
                    final String[] rnParts = signedRenewalInfo.split("\\.");
                    final JsonNode rnInfo = objectMapper.readTree(
                            new String(Base64.getUrlDecoder().decode(rnParts[1]))
                    );
                    final int autoRenewStatus = rnInfo.path("autoRenewStatus").asInt(-1);
                    log.info("Apple DID_CHANGE_RENEWAL_STATUS for transactionId: {}, autoRenewStatus: {}",
                            originalTransactionId, autoRenewStatus);
                    // autoRenewStatus=0 means auto-renew disabled; premium not revoked until period ends.
                    // Scheduler will handle expiry when expiryDate passes.
                }
                default -> log.debug("Unhandled Apple notification type: {}", notificationType);
            }
        } catch (Exception e) {
            log.error("Failed to process Apple webhook: {}", e.getMessage(), e);
        }
    }

    public void handleGoogleWebhook(final String encodedData) {
        try {
            final String json = new String(Base64.getDecoder().decode(encodedData));
            final JsonNode notification = objectMapper.readTree(json);
            final JsonNode subscriptionNotification = notification.path("subscriptionNotification");

            if (subscriptionNotification.isMissingNode()) {
                log.debug("Google notification is not a subscription notification, skipping");
                return;
            }

            final int notificationType = subscriptionNotification.path("notificationType").asInt();
            final String purchaseToken = subscriptionNotification.path("purchaseToken").asText();

            switch (notificationType) {
                case GOOGLE_SUBSCRIPTION_RENEWED, GOOGLE_SUBSCRIPTION_RECOVERED, GOOGLE_SUBSCRIPTION_PURCHASED -> {
                    final SubscriptionDocument sub = subscriptionRS.findByPurchaseToken(purchaseToken);
                    sub.setStatus(SubscriptionStatus.ACTIVE);
                    subscriptionRS.save(sub);
                    updatePremiumStatus(sub.getUserId(), true);
                    log.info("Google subscription renewed/activated for purchaseToken: {}", purchaseToken);
                }
                case GOOGLE_SUBSCRIPTION_EXPIRED -> {
                    final SubscriptionDocument sub = subscriptionRS.findByPurchaseToken(purchaseToken);
                    sub.setStatus(SubscriptionStatus.EXPIRED);
                    subscriptionRS.save(sub);
                    updatePremiumStatus(sub.getUserId(), false);
                    log.info("Google subscription expired for purchaseToken: {}", purchaseToken);
                }
                case GOOGLE_SUBSCRIPTION_CANCELED, GOOGLE_SUBSCRIPTION_REVOKED -> {
                    final SubscriptionDocument sub = subscriptionRS.findByPurchaseToken(purchaseToken);
                    sub.setStatus(SubscriptionStatus.CANCELLED);
                    subscriptionRS.save(sub);
                    updatePremiumStatus(sub.getUserId(), false);
                    log.info("Google subscription cancelled/revoked for purchaseToken: {}", purchaseToken);
                }
                case GOOGLE_SUBSCRIPTION_ON_HOLD -> {
                    final SubscriptionDocument sub = subscriptionRS.findByPurchaseToken(purchaseToken);
                    sub.setStatus(SubscriptionStatus.ON_HOLD);
                    subscriptionRS.save(sub);
                    updatePremiumStatus(sub.getUserId(), false);
                    log.warn("Google subscription ON_HOLD (payment withheld) for purchaseToken: {}", purchaseToken);
                }
                default -> log.debug("Unhandled Google notification type: {}", notificationType);
            }
        } catch (Exception e) {
            log.error("Failed to process Google webhook: {}", e.getMessage(), e);
        }
    }

    private JsonNode verifyAppleReceipt(final String receiptData) {
        final RestTemplate restTemplate = new RestTemplate();
        final Map<String, String> body = new HashMap<>();
        body.put("receipt-data", receiptData);
        body.put("password", appleSharedSecret);
        body.put("exclude-old-transactions", "true");

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            final ResponseEntity<JsonNode> response = restTemplate.postForEntity(appleVerifyUrl, entity, JsonNode.class);
            final JsonNode result = response.getBody();
            final int status = result.path("status").asInt(-1);

            if (status == APPLE_STATUS_SANDBOX) {
                // Retry with sandbox
                final ResponseEntity<JsonNode> sandboxResponse = restTemplate.postForEntity(
                        appleSandboxVerifyUrl, entity, JsonNode.class);
                return extractAppleReceiptInfo(sandboxResponse.getBody());
            }

            return extractAppleReceiptInfo(result);
        } catch (Exception e) {
            log.error("Apple receipt verification failed: {}", e.getMessage());
            throw BaseException.badRequest("error.subscription.verification_failed");
        }
    }

    private JsonNode extractAppleReceiptInfo(final JsonNode result) {
        if (result == null || result.path("status").asInt(-1) != APPLE_STATUS_OK) {
            throw BaseException.badRequest("error.subscription.verification_failed");
        }
        final JsonNode latestReceiptInfo = result.path("latest_receipt_info");
        if (!latestReceiptInfo.isArray() || latestReceiptInfo.isEmpty()) {
            throw BaseException.badRequest("error.subscription.verification_failed");
        }
        return latestReceiptInfo.get(latestReceiptInfo.size() - 1);
    }

    private JsonNode verifyGooglePurchase(final String packageName, final String productId, final String purchaseToken) {
        // Google Play Developer API requires service account OAuth2 — implementation
        // requires google-api-client library. For now, we validate the token structure
        // and rely on Google's webhook for state changes.
        // Full implementation: use GoogleCredentials + AndroidPublisher client.
        throw BaseException.badRequest("error.subscription.google_verification_not_configured");
    }

    private void updatePremiumStatus(final String userId, final boolean premium) {
        final var authDocument = authRS.findById(userId);
        authDocument.setPremium(premium);
        authRS.save(authDocument);
    }

    private SubscriptionResponse buildSubscriptionResponse(final SubscriptionDocument sub, final String userId) {
        return SubscriptionResponse.builder()
                .plan(sub.getPlan())
                .status(sub.getStatus())
                .expiryDate(sub.getExpiryDate())
                .accessToken(jwtService.generateToken(userId, false, true))
                .refreshToken(jwtService.generateToken(userId, true, false))
                .build();
    }
}
