package dev.batuhanyetgin.debttracker.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateCategoryResponse {

    private String id;
    private String name;
}
