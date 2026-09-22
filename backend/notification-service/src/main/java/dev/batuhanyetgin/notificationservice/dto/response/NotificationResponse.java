package dev.batuhanyetgin.notificationservice.dto.response;

import dev.batuhanyetgin.notificationservice.document.NotificationDocument;
import dev.batuhanyetgin.notificationservice.document.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private String id;
    private NotificationType type;
    private String title;
    private String body;
    private String entityId;
    private boolean read;
    private LocalDateTime createdDate;

    public static NotificationResponse from(final NotificationDocument document) {
        return NotificationResponse.builder()
                .id(document.getId())
                .type(document.getType())
                .title(document.getTitle())
                .body(document.getBody())
                .entityId(document.getEntityId())
                .read(document.isRead())
                .createdDate(document.getCreatedDate())
                .build();
    }
}
