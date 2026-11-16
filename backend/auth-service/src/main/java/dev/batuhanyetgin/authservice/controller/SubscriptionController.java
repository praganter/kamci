package dev.batuhanyetgin.authservice.controller;

import tools.jackson.databind.JsonNode;
import dev.batuhanyetgin.authservice.dto.response.SubscriptionResponse;
import dev.batuhanyetgin.authservice.service.SubscriptionService;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("subscription/")
@RequiredArgsConstructor
@Tag(name = "Subscription", description = "In-app subscription management")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    @Value("${ADMIN_SECRET_KEY:}")
    private String adminSecretKey;
    @Value("${REVENUECAT_WEBHOOK_SECRET:}")
    private String revenueCatWebhookSecret;

    @Operation(summary = "Get current subscription status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subscription status returned"),
            @ApiResponse(responseCode = "404", description = "No subscription found")
    })
    @GetMapping("status")
    ResponseEntity<SubscriptionResponse> status() {
        final String userId = RequestContextUtil.getUserId();
        return ResponseEntity.ok(subscriptionService.getStatus(userId));
    }

    @Operation(summary = "RevenueCat webhook (public)")
    @ApiResponse(responseCode = "200", description = "Event processed")
    @PostMapping("revenuecat/webhook")
    ResponseEntity<Void> revenueCatWebhook(
            @RequestHeader(value = "Authorization", required = false) final String authHeader,
            @RequestBody final JsonNode payload) {
        validateRevenueCatSecret(authHeader);
        subscriptionService.handleRevenueCatWebhook(payload);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Grant premium to a user")
    @PostMapping("premium/{userId}")
    ResponseEntity<Void> grantPremium(
            @RequestHeader("X-Admin-Key") final String adminKey,
            @PathVariable final String userId) {
        validateAdminKey(adminKey);
        subscriptionService.grantPremium(userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Revoke premium from a user")
    @DeleteMapping("premium/{userId}")
    ResponseEntity<Void> revokePremium(
            @RequestHeader("X-Admin-Key") final String adminKey,
            @PathVariable final String userId) {
        validateAdminKey(adminKey);
        subscriptionService.revokePremium(userId);
        return ResponseEntity.ok().build();
    }

    private void validateAdminKey(final String adminKey) {
        if (adminSecretKey.isBlank() || !MessageDigest.isEqual(
                adminSecretKey.getBytes(StandardCharsets.UTF_8),
                adminKey.getBytes(StandardCharsets.UTF_8))) {
            throw BaseException.forbidden("error.admin.unauthorized");
        }
    }

    private void validateRevenueCatSecret(final String authHeader) {
        final String expected = "Bearer " + revenueCatWebhookSecret;
        if (revenueCatWebhookSecret.isBlank() || authHeader == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                authHeader.getBytes(StandardCharsets.UTF_8))) {
            throw BaseException.forbidden("error.subscription.webhook_unauthorized");
        }
    }
}
