package dev.batuhanyetgin.debttracker.dto.request;

import dev.batuhanyetgin.debttracker.enums.AccessLevelType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddMemberRequest {

    @Email(message = "{error.validation.email}")
    @NotBlank(message = "{error.validation.not_blank}")
    private String email;

    @NotNull(message = "{error.validation.not_null}")
    private AccessLevelType accessLevel;
}
