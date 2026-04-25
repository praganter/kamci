package dev.batuhanyetgin.notificationservice.controller;

import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import dev.batuhanyetgin.notificationservice.document.DeviceTokenDocument;
import dev.batuhanyetgin.notificationservice.dto.request.DeviceTokenRequest;
import dev.batuhanyetgin.notificationservice.repository.DeviceTokenRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("notification/")
@RequiredArgsConstructor
@Tag(name = "Device Token", description = "FCM device token registration and removal")
public class DeviceTokenController {

    private final DeviceTokenRepository deviceTokenRepository;
    private final dev.batuhanyetgin.notificationservice.service.FcmService fcmService;

    @Operation(summary = "Send a test FCM notification to a specific user")
    @PostMapping("test")
    ResponseEntity<Void> sendTestNotification(@RequestBody @Valid final dev.batuhanyetgin.notificationservice.dto.request.NotificationTestRequest request) {
        log.info("Sending test notification to userId: {}", request.getUserId());
        deviceTokenRepository.findByUserId(request.getUserId())
                .ifPresentOrElse(
                        doc -> fcmService.send(doc.getFcmToken(), request.getTitle(), request.getBody(), java.util.Map.of("test", "true")),
                        () -> {
                            throw new RuntimeException("Device token not found for user: " + request.getUserId());
                        }
                );
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Register or update the FCM device token for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Device token saved"),
            @ApiResponse(responseCode = "422", description = "Validation error")
    })
    @PatchMapping("device-token")
    ResponseEntity<Void> registerDeviceToken(@RequestBody @Valid final DeviceTokenRequest request) {
        final String userId = RequestContextUtil.getUserId();
        final String locale = RequestContextUtil.getLocale();
        log.info("Registering device token for userId: {}", userId);
        deviceTokenRepository.upsertByUserId(userId, request.getToken(), locale, request.getEmail());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove the FCM device token for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Device token removed")
    })
    @DeleteMapping("device-token")
    ResponseEntity<Void> deleteDeviceToken() {
        final String userId = RequestContextUtil.getUserId();
        log.info("Deleting device token for userId: {}", userId);
        deviceTokenRepository.deleteByUserId(userId);
        return ResponseEntity.noContent().build();
    }
}