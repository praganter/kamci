package dev.batuhanyetgin.kamci.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCategoryRequest {

    @NotBlank(message = "{error.validation.not_blank}")
    @Size(max = 24, message = "{error.validation.size}")
    private String id;

    @NotBlank(message = "{error.validation.not_blank}")
    @Size(max = 150, message = "{error.validation.size}")
    private String name;
}
