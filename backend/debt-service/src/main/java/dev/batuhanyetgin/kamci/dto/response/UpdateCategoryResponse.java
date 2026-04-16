package dev.batuhanyetgin.kamci.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateCategoryResponse {

    private String id;
    private String name;
}
