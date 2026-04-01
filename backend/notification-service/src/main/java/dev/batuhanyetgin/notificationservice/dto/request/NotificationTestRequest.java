package dev.batuhanyetgin.notificationservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTestRequest {
    @NotBlank
    private String userId;
    @NotBlank
    private String title;
    @NotBlank
    private String body;
}
