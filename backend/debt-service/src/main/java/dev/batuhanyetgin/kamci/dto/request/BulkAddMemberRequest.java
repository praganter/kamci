package dev.batuhanyetgin.kamci.dto.request;

import dev.batuhanyetgin.kamci.enums.AccessLevelType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BulkAddMemberRequest {

    @NotEmpty(message = "{error.validation.not_empty}")
    private List<@Size(max = 24, message = "{error.validation.size}") String> categoryIds;

    @Email(message = "{error.validation.email}")
    @NotBlank(message = "{error.validation.not_blank}")
    private String email;

    @NotNull(message = "{error.validation.not_null}")
    private AccessLevelType accessLevel;
}
