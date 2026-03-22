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
        deviceTokenRepository.findByUserId(userId)
                .ifPresentOrElse(
                        doc -> {
                            doc.setFcmToken(request.getToken());
                            doc.setLocale(locale);
                            deviceTokenRepository.save(doc);
                        },
                        () -> deviceTokenRepository.save(
                                DeviceTokenDocument.builder()
                                        .userId(userId)
                                        .fcmToken(request.getToken())
                                        .locale(locale)
                                        .build())
                );
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
