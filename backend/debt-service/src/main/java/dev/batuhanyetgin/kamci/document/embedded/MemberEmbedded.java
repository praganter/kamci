package dev.batuhanyetgin.kamci.document.embedded;

import dev.batuhanyetgin.kamci.enums.AccessLevelType;
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

    private String email;

    private AccessLevelType accessLevel;
}