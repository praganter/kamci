package dev.batuhanyetgin.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RegisterRequest {

    @Email(message = "{error.validation.email}")
    @NotBlank(message = "{error.validation.not_blank}")
    @Size(max = 100, message = "{error.validation.size}")
    private String email;

    @NotBlank(message = "{error.validation.not_blank}")
    @Size(min = 8, max = 72, message = "{error.validation.password.size}")
    private String password;
}