package dev.batuhanyetgin.debttracker.document.embedded;

import dev.batuhanyetgin.commonmodule.document.BaseMongoDocument;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CategoryEmbedded extends BaseMongoDocument {

    private String name;
}