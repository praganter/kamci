package dev.batuhanyetgin.notificationservice.controller;

import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import dev.batuhanyetgin.notificationservice.dto.response.NotificationPageResponse;
import dev.batuhanyetgin.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("notification/")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "Persistent in-app notification list")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "List the authenticated user's notifications, newest first")
    @GetMapping("list")
    ResponseEntity<NotificationPageResponse> list(
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") final int size) {
        final String userId = RequestContextUtil.getUserId();
        final var result = notificationService.list(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(NotificationPageResponse.from(result));
    }

    @Operation(summary = "Mark a single notification as read")
    @PatchMapping("{id}/read")
    ResponseEntity<Void> markAsRead(@PathVariable final String id) {
        final String userId = RequestContextUtil.getUserId();
        notificationService.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }
}
