package dev.batuhanyetgin.authservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.batuhanyetgin.authservice.document.SubscriptionDocument;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final AuthRepositoryService authRS;
    private final SubscriptionRepositoryService subscriptionRS;
    private final ObjectMapper objectMapper;

    public SubscriptionResponse getStatus(final String userId) {
        return subscriptionRS.findByUserId(userId)
                .map(sub -> SubscriptionResponse.builder()
                        .plan(sub.getPlan())
                        .status(sub.getStatus())
                        .expiryDate(sub.getExpiryDate())
                        .build())
                .orElseThrow(() -> BaseException.notFound("error.subscription.not_found"));
    }

    public void handleRevenueCatWebhook(final JsonNode payload) {
        try {
            final JsonNode event = payload.path("event");
            final String type = event.path("type").asText();
            final String userId = event.path("app_user_id").asText();
            if (userId.isBlank()) {
                log.warn("RevenueCat webhook event {} missing app_user_id", type);
                return;
            }

            final SubscriptionDocument sub = subscriptionRS.findByUserId(userId)
                    .orElseGet(() -> SubscriptionDocument.builder().userId(userId).build());
            final boolean isNew = sub.getId() == null;
            if (isNew) {
                sub.setStartDate(LocalDateTime.now(ZoneOffset.UTC));
            }

            final String productId = event.path("product_id").asText(null);
            if (productId != null) {
                sub.setProductId(productId);
            }
            final String store = event.path("store").asText(null);
            if (store != null) {
                sub.setPlatform(mapStore(store));
            }
            final long expiresMs = event.path("expiration_at_ms").asLong(-1);
            if (expiresMs > 0) {
                sub.setExpiryDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(expiresMs), ZoneOffset.UTC));
            }

            switch (type) {
                case "INITIAL_PURCHASE", "RENEWAL", "UNCANCELLATION", "PRODUCT_CHANGE" -> {
                    sub.setStatus(SubscriptionStatus.ACTIVE);
                    subscriptionRS.save(sub);
                    updatePremiumStatus(userId, true);
                    log.info("RevenueCat {} → subscription ACTIVE for userId: {}", type, userId);
                }
                case "EXPIRATION" -> {
                    sub.setStatus(SubscriptionStatus.EXPIRED);
                    subscriptionRS.save(sub);
                    updatePremiumStatus(userId, false);
                    log.info("RevenueCat EXPIRATION → subscription EXPIRED for userId: {}", userId);
                }
                case "BILLING_ISSUE" -> {
                    sub.setStatus(SubscriptionStatus.ON_HOLD);
                    subscriptionRS.save(sub);
                    log.warn("RevenueCat BILLING_ISSUE for userId: {}", userId);
                }
                case "CANCELLATION" -> {
                    // Auto-renew turned off; premium stays active until expiryDate.
                    // SubscriptionExpiryScheduler revokes premium once it passes.
                    subscriptionRS.save(sub);
                    log.info("RevenueCat CANCELLATION (auto-renew off) for userId: {}", userId);
                }
                default -> log.debug("Unhandled RevenueCat event type: {}", type);
            }
        } catch (Exception e) {
            log.error("Failed to process RevenueCat webhook: {}", e.getMessage(), e);
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

    private SubscriptionPlatform mapStore(final String store) {
        return switch (store) {
            case "APP_STORE" -> SubscriptionPlatform.IOS;
            case "PLAY_STORE" -> SubscriptionPlatform.ANDROID;
            default -> SubscriptionPlatform.ADMIN;
        };
    }

    private void updatePremiumStatus(final String userId, final boolean premium) {
        final var authDocument = authRS.findById(userId);
        authDocument.setPremium(premium);
        authRS.save(authDocument);
    }
}
