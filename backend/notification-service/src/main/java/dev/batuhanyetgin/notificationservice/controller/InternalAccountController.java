package dev.batuhanyetgin.notificationservice.controller;

import dev.batuhanyetgin.notificationservice.repository.DeviceTokenRepository;
import dev.batuhanyetgin.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("notification/internal/")
@RequiredArgsConstructor
@Tag(name = "Internal", description = "Inter-service endpoints, not exposed through the gateway")
public class InternalAccountController {

    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationService notificationService;

    @Operation(summary = "Delete device token and notification history for a user, called by auth-service on account deletion")
    @DeleteMapping("account-data/{userId}")
    ResponseEntity<Void> deleteAccountData(@PathVariable final String userId) {
        deviceTokenRepository.deleteByUserId(userId);
        notificationService.deleteAllForUser(userId);
        return ResponseEntity.noContent().build();
    }
}
