package dev.batuhanyetgin.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "{error.validation.not_blank}")
    @Size(min = 8, message = "{error.validation.password.size}")
    private String password;

    @NotBlank(message = "{error.validation.not_blank}")
    @Email(message = "{error.validation.email}")
    private String email;
}