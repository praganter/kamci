package dev.batuhanyetgin.authservice.controller;

import dev.batuhanyetgin.authservice.dto.request.VerifyReceiptRequest;
import dev.batuhanyetgin.authservice.dto.response.SubscriptionResponse;
import dev.batuhanyetgin.authservice.service.SubscriptionService;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("subscription/")
@RequiredArgsConstructor
@Tag(name = "Subscription", description = "In-app subscription management")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    @Value("${ADMIN_SECRET_KEY:}")
    private String adminSecretKey;

    @Operation(summary = "Verify a purchase receipt and activate premium")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subscription activated, new tokens returned"),
            @ApiResponse(responseCode = "400", description = "Receipt verification failed"),
            @ApiResponse(responseCode = "422", description = "Validation error")
    })
    @PostMapping("verify")
    ResponseEntity<SubscriptionResponse> verify(@RequestBody @Valid final VerifyReceiptRequest request) {
        final String userId = RequestContextUtil.getUserId();
        return ResponseEntity.ok(subscriptionService.verifyReceipt(userId, request));
    }

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

    @Operation(summary = "Apple App Store Server Notification webhook (public, authenticated by its JWS signature)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification processed or ignored"),
            @ApiResponse(responseCode = "400", description = "No signedPayload"),
            @ApiResponse(responseCode = "403", description = "Invalid signature, other app or unknown environment"),
            @ApiResponse(responseCode = "503", description = "Apple verification is not configured")
    })
    @PostMapping("apple/webhook")
    ResponseEntity<Void> appleWebhook(@RequestBody final Map<String, String> payload) {
        final String signedPayload = payload.get("signedPayload");
        if (signedPayload == null) {
            throw BaseException.badRequest("error.subscription.verification_failed");
        }
        subscriptionService.handleAppleWebhook(signedPayload);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Google Play real-time developer notification (public, authenticated by the Pub/Sub OIDC token)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification processed or ignored"),
            @ApiResponse(responseCode = "400", description = "Malformed notification"),
            @ApiResponse(responseCode = "403", description = "Missing or invalid Pub/Sub token, or not configured"),
            @ApiResponse(responseCode = "503", description = "Google verification is not configured")
    })
    @PostMapping("google/webhook")
    ResponseEntity<Void> googleWebhook(
            @RequestHeader(value = "Authorization", required = false) final String authorization,
            @RequestBody final Map<String, Object> payload) {
        String data = null;
        if (payload.get("message") instanceof Map<?, ?> message && message.get("data") instanceof String encoded) {
            data = encoded;
        }
        subscriptionService.handleGoogleWebhook(authorization, data);
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
}