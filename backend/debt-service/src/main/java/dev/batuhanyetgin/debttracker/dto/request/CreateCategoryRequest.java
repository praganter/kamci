package dev.batuhanyetgin.debttracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCategoryRequest {

    @NotBlank(message = "{error.validation.not_blank}")
    private String name;
}