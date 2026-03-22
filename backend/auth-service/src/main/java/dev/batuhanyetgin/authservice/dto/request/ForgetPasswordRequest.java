package dev.batuhanyetgin.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgetPasswordRequest {

    @Email(message = "{error.validation.email}")
    @NotBlank(message = "{error.validation.not_blank}")
    @Size(max = 100, message = "{error.validation.size}")
    private String email;
}