package dev.batuhanyetgin.debttracker.dto.request;

import dev.batuhanyetgin.debttracker.enums.AccessLevelType;
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

    @NotBlank(message = "{error.validation.not_blank}")
    @Size(max = 24, message = "{error.validation.size}")
    private String userId;

    @NotNull(message = "{error.validation.not_null}")
    private AccessLevelType accessLevel;
}
