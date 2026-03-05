package dev.batuhanyetgin.debttracker.document.embedded;

import dev.batuhanyetgin.commonmodule.document.BaseMongoDocument;
import dev.batuhanyetgin.debttracker.enums.AccessLevelType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SharedWithEmbedded extends BaseMongoDocument {

    private String userId;

    private AccessLevelType accessLevelType;
}