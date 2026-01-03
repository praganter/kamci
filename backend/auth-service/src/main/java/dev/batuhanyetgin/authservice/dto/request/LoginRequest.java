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
    @NotBlank
    @Size(min = 8)
    private String password;

    @Email
    private String email;
}