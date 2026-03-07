package dev.batuhanyetgin.debttracker.document.embedded;

import dev.batuhanyetgin.commonmodule.document.BaseMongoDocument;
import dev.batuhanyetgin.debttracker.enums.AccessLevelType;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedWithEmbedded extends BaseMongoDocument {

    private String userId;

    private AccessLevelType accessLevelType;
}