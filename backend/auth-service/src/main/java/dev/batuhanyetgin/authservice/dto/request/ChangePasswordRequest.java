package dev.batuhanyetgin.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank(message = "{error.validation.not_blank}")
    @Size(min = 8, max = 72, message = "{error.validation.password.size}")
    private String currentPassword;

    @NotBlank(message = "{error.validation.not_blank}")
    @Size(min = 8, max = 72, message = "{error.validation.password.size}")
    private String newPassword;
}
