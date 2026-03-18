package dev.batuhanyetgin.debttracker.document.embedded;

import dev.batuhanyetgin.debttracker.enums.AccessLevelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberEmbedded {

    private String userId;

    private AccessLevelType accessLevel;
}