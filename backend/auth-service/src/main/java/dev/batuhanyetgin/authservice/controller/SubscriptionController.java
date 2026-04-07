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

    @Operation(summary = "Apple App Store Server Notification webhook (public)")
    @ApiResponse(responseCode = "200", description = "Notification processed")
    @PostMapping("apple/webhook")
    ResponseEntity<Void> appleWebhook(@RequestBody final Map<String, String> payload) {
        final String signedPayload = payload.get("signedPayload");
        if (signedPayload != null) {
            subscriptionService.handleAppleWebhook(signedPayload);
        }
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Google Play Real-time Developer Notification webhook (public)")
    @ApiResponse(responseCode = "200", description = "Notification processed")
    @PostMapping("google/webhook")
    ResponseEntity<Void> googleWebhook(@RequestBody final Map<String, Object> payload) {
        try {
            @SuppressWarnings("unchecked") final Map<String, String> message = (Map<String, String>) payload.get("message");
            if (message != null) {
                final String data = message.get("data");
                if (data != null) {
                    subscriptionService.handleGoogleWebhook(data);
                }
            }
        } catch (ClassCastException e) {
            // Malformed payload — ignore
        }
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
        if (adminSecretKey.isBlank() || !adminSecretKey.equals(adminKey)) {
            throw BaseException.forbidden("error.admin.unauthorized");
        }
    }
}