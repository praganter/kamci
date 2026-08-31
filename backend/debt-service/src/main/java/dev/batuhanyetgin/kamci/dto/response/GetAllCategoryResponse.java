package dev.batuhanyetgin.kamci.dto.response;

import dev.batuhanyetgin.kamci.enums.AccessLevelType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GetAllCategoryResponse {

    private String id;
    private String name;
    private String ownerId;
    private List<MemberItem> members;

    @Getter
    @Builder
    public static class MemberItem {
        private String userId;
        private String email;
        private AccessLevelType accessLevel;
    }
}