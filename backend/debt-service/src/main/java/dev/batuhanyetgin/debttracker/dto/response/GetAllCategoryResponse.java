package dev.batuhanyetgin.debttracker.dto.response;

import dev.batuhanyetgin.debttracker.enums.AccessLevelType;
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
        private AccessLevelType accessLevel;
    }
}