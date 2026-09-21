package dev.batuhanyetgin.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteHouseholdRequest {

    @Email(message = "{error.validation.email}")
    @NotBlank(message = "{error.validation.not_blank}")
    private String email;
}
