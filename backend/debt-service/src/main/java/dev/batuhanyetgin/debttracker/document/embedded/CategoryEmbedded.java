package dev.batuhanyetgin.debttracker.document.embedded;

import dev.batuhanyetgin.commonmodule.document.BaseMongoDocument;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CategoryEmbedded extends BaseMongoDocument {

    @NotBlank(message = "{error.validation.not_blank}")
    private String name;
}