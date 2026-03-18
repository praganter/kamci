package dev.batuhanyetgin.debttracker.dto.request;

import dev.batuhanyetgin.debttracker.enums.AccessLevelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BulkAddMemberRequest {

    @NotEmpty(message = "{error.validation.not_empty}")
    private List<String> categoryIds;

    @NotBlank(message = "{error.validation.not_blank}")
    private String userId;

    @NotNull(message = "{error.validation.not_null}")
    private AccessLevelType accessLevel;
}
