package dev.batuhanyetgin.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "{error.validation.not_blank}")
    private String token;

    @NotBlank(message = "{error.validation.not_blank}")
    @Size(min = 8, max = 72, message = "{error.validation.password.size}")
    private String newPassword;
}