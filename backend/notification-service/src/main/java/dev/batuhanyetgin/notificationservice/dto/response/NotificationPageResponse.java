package dev.batuhanyetgin.notificationservice.dto.response;

import dev.batuhanyetgin.notificationservice.document.NotificationDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPageResponse {
    private List<NotificationResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static NotificationPageResponse from(final Page<NotificationDocument> page) {
        return NotificationPageResponse.builder()
                .content(page.getContent().stream().map(NotificationResponse::from).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
