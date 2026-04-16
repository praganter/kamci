package dev.batuhanyetgin.kamci.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SaveCategoryResponse {

    private String id;
    private String name;
}