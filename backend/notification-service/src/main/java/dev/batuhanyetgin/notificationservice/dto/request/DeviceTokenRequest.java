package dev.batuhanyetgin.notificationservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeviceTokenRequest {

    @NotBlank(message = "{error.validation.not_blank}")
    @Size(max = 256, message = "{error.validation.size}")
    private String token;
}
