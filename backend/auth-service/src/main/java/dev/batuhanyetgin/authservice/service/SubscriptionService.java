package dev.batuhanyetgin.authservice.service;

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
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    // Google Play subscription notification types
    private static final int GOOGLE_SUBSCRIPTION_RECOVERED = 1;
    private static final int GOOGLE_SUBSCRIPTION_RENEWED = 4;
    private static final int GOOGLE_SUBSCRIPTION_PURCHASED = 7;
    private static final int GOOGLE_SUBSCRIPTION_CANCELED = 3;
    private static final int GOOGLE_SUBSCRIPTION_ON_HOLD = 5;
    private static final int GOOGLE_SUBSCRIPTION_EXPIRED = 13;
    private static final int GOOGLE_SUBSCRIPTION_REVOKED = 12;

    private final AuthRepositoryService authRS;
    private final SubscriptionRepositoryService subscriptionRS;
    private final JwtService jwtService;
    private final JsonMapper jsonMapper;
    private final AppleJwsVerifier appleJwsVerifier;
    private final GooglePlayVerifier googlePlayVerifier;
    private final AppleReceiptVerifier appleReceiptVerifier;
    private final PubSubTokenVerifier pubSubTokenVerifier;
    private final IapSettings settings;

    public SubscriptionResponse verifyReceipt(final String userId, final VerifyReceiptRequest request) {
        final SubscriptionPlatform platform = SubscriptionPlatform.valueOf(request.getPlatform());
        final SubscriptionPlan plan = SubscriptionPlan.valueOf(request.getPlan());

        requireProductForPlan(platform, request.getProductId(), plan);

        final LocalDateTime expiryDate;
        final String transactionId;

        if (platform == SubscriptionPlatform.IOS) {
            if (!appleReceiptVerifier.isConfigured()) {
                throw BaseException.serviceUnavailable("error.subscription.apple_not_configured");
            }
            final JsonNode receiptInfo = appleReceiptVerifier.verify(request.getReceiptData());
            if (!request.getProductId().equals(receiptInfo.path("product_id").asString(null))) {
                throw BaseException.badRequest("error.subscription.verification_failed");
            }
            transactionId = receiptInfo.path("original_transaction_id").asString();
            expiryDate = toUtc(receiptInfo.path("expires_date_ms").asLong());
            requireNotAlreadyUsedByAnotherUser(userId, subscriptionRS.findOptionalByOriginalTransactionId(transactionId));
        } else {
            if (!settings.isGoogleConfigured() || !googlePlayVerifier.isConfigured()) {
                throw BaseException.serviceUnavailable("error.subscription.google_not_configured");
            }
            final String packageName = resolvePackageName(request.getPackageName());
            final JsonNode purchaseInfo = verifyGooglePurchase(
                    packageName,
                    request.getProductId(),
                    request.getReceiptData()
            );
            transactionId = purchaseInfo.path("orderId").asString();
            expiryDate = toUtc(purchaseInfo.path("expiryTimeMillis").asLong());
            requireNotAlreadyUsedByAnotherUser(userId, subscriptionRS.findOptionalByPurchaseToken(request.getReceiptData()));
        }

        if (!expiryDate.isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            throw BaseException.badRequest("error.subscription.verification_failed");
        }

        final SubscriptionDocument subscription = subscriptionRS.findByUserId(userId)
                .orElse(SubscriptionDocument.builder().userId(userId).build());

        final boolean isNew = subscription.getId() == null;

        subscription.setPlan(plan);
        subscription.setPlatform(platform);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setProductId(request.getProductId());
        if (isNew) {
            subscription.setStartDate(LocalDateTime.now(ZoneOffset.UTC));
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

    /**
     * Apple App Store Server Notification V2. The JWS signature is the only authentication, so an
     * invalid signature, another app's bundle id or an unknown environment is a 403.
     */
    public void handleAppleWebhook(final String signedPayload) {
        if (!settings.isAppleConfigured()) {
            throw BaseException.serviceUnavailable("error.subscription.apple_not_configured");
        }

        final JsonNode payload = verifyApple(signedPayload);
        final String notificationType = payload.path("notificationType").asString("");
        final JsonNode data = payload.path("data");
        if (data.isMissingNode() || data.isNull()) {
            log.debug("Apple notification {} carries no data, skipping", notificationType);
            return;
        }

        final String environment = data.path("environment").asString("");
        if (!requireAllowedAppleEnvironment(environment)) {
            return;
        }
        requireAppleBundle(data.path("bundleId").asString(null));

        final JsonNode txInfo = verifyApple(data.path("signedTransactionInfo").asString(""));
        requireAppleBundle(txInfo.path("bundleId").asString(null));
        if (!environment.equals(txInfo.path("environment").asString(""))) {
            throw BaseException.forbidden("error.subscription.webhook_unauthorized");
        }

        final long signedDate = payload.path("signedDate").asLong(0);
        if (signedDate <= 0) {
            throw BaseException.forbidden("error.subscription.webhook_unauthorized");
        }

        final String originalTransactionId = txInfo.path("originalTransactionId").asString("");
        final Optional<SubscriptionDocument> found = subscriptionRS.findOptionalByOriginalTransactionId(originalTransactionId);
        if (found.isEmpty()) {
            log.warn("Apple {} for an unknown subscription transactionId: {}", notificationType, originalTransactionId);
            return;
        }
        final SubscriptionDocument sub = found.get();
        if (isStale(sub, signedDate)) {
            log.info("Ignoring stale or replayed Apple {} for transactionId: {}", notificationType, originalTransactionId);
            return;
        }

        final long expiresMs = txInfo.path("expiresDate").asLong(0);
        final LocalDateTime expiryDate = expiresMs > 0 ? toUtc(expiresMs) : null;

        switch (notificationType) {
            case "SUBSCRIBED", "DID_RENEW" -> {
                sub.setStatus(SubscriptionStatus.ACTIVE);
                if (expiryDate != null) sub.setExpiryDate(expiryDate);
                applyNotification(sub, signedDate, true);
                log.info("Apple subscription renewed/activated for transactionId: {}", originalTransactionId);
            }
            case "EXPIRED", "DID_FAIL_TO_RENEW", "REFUND" -> {
                sub.setStatus("REFUND".equals(notificationType)
                        ? SubscriptionStatus.CANCELLED : SubscriptionStatus.EXPIRED);
                applyNotification(sub, signedDate, false);
                log.info("Apple subscription expired/cancelled for transactionId: {}", originalTransactionId);
            }
            case "GRACE_PERIOD_EXPIRED" -> {
                sub.setStatus(SubscriptionStatus.EXPIRED);
                applyNotification(sub, signedDate, false);
                log.info("Apple grace period expired for transactionId: {}", originalTransactionId);
            }
            case "DID_CHANGE_RENEWAL_STATUS" -> {
                final JsonNode rnInfo = verifyApple(data.path("signedRenewalInfo").asString(""));
                log.info("Apple DID_CHANGE_RENEWAL_STATUS for transactionId: {}, autoRenewStatus: {}",
                        originalTransactionId, rnInfo.path("autoRenewStatus").asInt(-1));
                // autoRenewStatus=0 means auto-renew disabled; premium stays until the period ends and
                // the expiry scheduler revokes it afterwards.
                sub.setLastNotificationMs(signedDate);
                subscriptionRS.save(sub);
            }
            default -> log.debug("Unhandled Apple notification type: {}", notificationType);
        }
    }

    /**
     * Google Play real-time developer notification, delivered by a Pub/Sub push subscription. The
     * push token (a Google-signed OIDC JWT) is the authentication; it is checked before anything in
     * the body is read.
     */
    public void handleGoogleWebhook(final String authorizationHeader, final String encodedData) {
        pubSubTokenVerifier.verify(authorizationHeader);
        if (!settings.isGoogleConfigured()) {
            throw BaseException.serviceUnavailable("error.subscription.google_not_configured");
        }
        if (encodedData == null) {
            log.debug("Google push without message data, skipping");
            return;
        }

        final JsonNode notification;
        try {
            notification = jsonMapper.readTree(new String(Base64.getDecoder().decode(encodedData), StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            throw BaseException.badRequest("error.subscription.verification_failed");
        }

        if (!settings.getGooglePackageName().equals(notification.path("packageName").asString(null))) {
            log.warn("Ignoring Google notification for another package");
            return;
        }
        final JsonNode subscriptionNotification = notification.path("subscriptionNotification");
        if (subscriptionNotification.isMissingNode()) {
            log.debug("Google notification is not a subscription notification, skipping");
            return;
        }

        final int notificationType = subscriptionNotification.path("notificationType").asInt();
        final String purchaseToken = subscriptionNotification.path("purchaseToken").asString("");
        final long eventTimeMs = notification.path("eventTimeMillis").asLong(0);
        if (eventTimeMs <= 0) {
            throw BaseException.badRequest("error.subscription.verification_failed");
        }

        final Optional<SubscriptionDocument> found = subscriptionRS.findOptionalByPurchaseToken(purchaseToken);
        if (found.isEmpty()) {
            log.warn("Google notification type {} for an unknown purchase token", notificationType);
            return;
        }
        final SubscriptionDocument sub = found.get();
        if (isStale(sub, eventTimeMs)) {
            log.info("Ignoring stale or replayed Google notification type {}", notificationType);
            return;
        }

        switch (notificationType) {
            case GOOGLE_SUBSCRIPTION_RENEWED, GOOGLE_SUBSCRIPTION_RECOVERED, GOOGLE_SUBSCRIPTION_PURCHASED -> {
                sub.setStatus(SubscriptionStatus.ACTIVE);
                applyNotification(sub, eventTimeMs, true);
                log.info("Google subscription renewed/activated for userId: {}", sub.getUserId());
            }
            case GOOGLE_SUBSCRIPTION_EXPIRED -> {
                sub.setStatus(SubscriptionStatus.EXPIRED);
                applyNotification(sub, eventTimeMs, false);
                log.info("Google subscription expired for userId: {}", sub.getUserId());
            }
            case GOOGLE_SUBSCRIPTION_CANCELED, GOOGLE_SUBSCRIPTION_REVOKED -> {
                sub.setStatus(SubscriptionStatus.CANCELLED);
                applyNotification(sub, eventTimeMs, false);
                log.info("Google subscription cancelled/revoked for userId: {}", sub.getUserId());
            }
            case GOOGLE_SUBSCRIPTION_ON_HOLD -> {
                sub.setStatus(SubscriptionStatus.ON_HOLD);
                applyNotification(sub, eventTimeMs, false);
                log.warn("Google subscription ON_HOLD (payment withheld) for userId: {}", sub.getUserId());
            }
            default -> log.debug("Unhandled Google notification type: {}", notificationType);
        }
    }

    public void grantPremium(final String userId) {
        final var authDocument = authRS.findById(userId);
        authDocument.setPremium(true);
        authRS.save(authDocument);

        final var subscription = subscriptionRS.findByUserId(userId)
                .orElseGet(() -> SubscriptionDocument.builder()
                        .userId(userId)
                        .build());

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPlan(SubscriptionPlan.YEARLY);
        subscription.setPlatform(SubscriptionPlatform.ADMIN);
        subscription.setStartDate(LocalDateTime.now(ZoneOffset.UTC));
        subscription.setExpiryDate(LocalDateTime.now(ZoneOffset.UTC).plusYears(100));
        subscription.setOriginalTransactionId("ADMIN_GRANT_" + userId);
        subscription.setPurchaseToken("ADMIN_GRANT_" + userId);

        subscriptionRS.save(subscription);
    }

    public void revokePremium(final String userId) {
        final var authDocument = authRS.findById(userId);
        authDocument.setPremium(false);
        authRS.save(authDocument);

        subscriptionRS.findByUserId(userId).ifPresent(subscription -> {
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            subscriptionRS.save(subscription);
        });
    }

    /** The product must be a known one and must belong to the plan the client claims. */
    private void requireProductForPlan(final SubscriptionPlatform platform, final String productId,
                                       final SubscriptionPlan plan) {
        if (!settings.productsConfigured()) {
            throw BaseException.serviceUnavailable(platform == SubscriptionPlatform.IOS
                    ? "error.subscription.apple_not_configured"
                    : "error.subscription.google_not_configured");
        }
        if (settings.planFor(productId).filter(plan::equals).isEmpty()) {
            throw BaseException.badRequest("error.subscription.product_not_allowed");
        }
    }

    private void requireNotAlreadyUsedByAnotherUser(final String userId, final Optional<SubscriptionDocument> existing) {
        existing.filter(sub -> !userId.equals(sub.getUserId())).ifPresent(sub -> {
            log.warn("Receipt of userId {} presented by another user {}", sub.getUserId(), userId);
            throw BaseException.conflict("error.subscription.already_used");
        });
    }

    /** Only the configured application may be verified; a client that omits the package gets it filled in. */
    private String resolvePackageName(final String requested) {
        if (requested == null || requested.isBlank()) {
            return settings.getGooglePackageName();
        }
        if (!settings.getGooglePackageName().equals(requested)) {
            throw BaseException.badRequest("error.subscription.package_not_allowed");
        }
        return requested;
    }

    private JsonNode verifyApple(final String signedJws) {
        try {
            return appleJwsVerifier.verifyAndDecode(signedJws);
        } catch (SecurityException e) {
            log.warn("Rejected Apple notification: {}", e.getMessage());
            throw BaseException.forbidden("error.subscription.webhook_unauthorized");
        }
    }

    private void requireAppleBundle(final String bundleId) {
        if (!settings.getAppleBundleId().equals(bundleId)) {
            log.warn("Rejected Apple notification for a different bundle id");
            throw BaseException.forbidden("error.subscription.webhook_unauthorized");
        }
    }

    /** True when the notification should be processed; false for a sandbox event that is not allowed. */
    private boolean requireAllowedAppleEnvironment(final String environment) {
        if ("Production".equals(environment)) {
            return true;
        }
        if ("Sandbox".equals(environment)) {
            if (settings.isAppleAllowSandbox()) {
                return true;
            }
            log.info("Ignoring Apple sandbox notification (APPLE_ALLOW_SANDBOX is off)");
            return false;
        }
        throw BaseException.forbidden("error.subscription.webhook_unauthorized");
    }

    private boolean isStale(final SubscriptionDocument sub, final long notificationMs) {
        return sub.getLastNotificationMs() != null && notificationMs <= sub.getLastNotificationMs();
    }

    private void applyNotification(final SubscriptionDocument sub, final long notificationMs, final boolean premium) {
        sub.setLastNotificationMs(notificationMs);
        subscriptionRS.save(sub);
        updatePremiumStatus(sub.getUserId(), premium);
    }

    private JsonNode verifyGooglePurchase(final String packageName, final String productId, final String purchaseToken) {
        try {
            final JsonNode result = googlePlayVerifier.verifySubscription(packageName, productId, purchaseToken);
            if (result == null) {
                throw BaseException.badRequest("error.subscription.verification_failed");
            }
            return result;
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google Play purchase verification failed: {}", e.getMessage());
            throw BaseException.badRequest("error.subscription.verification_failed");
        }
    }

    private void updatePremiumStatus(final String userId, final boolean premium) {
        final var authDocument = authRS.findById(userId);
        authDocument.setPremium(premium);
        authRS.save(authDocument);
    }

    private static LocalDateTime toUtc(final long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
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
