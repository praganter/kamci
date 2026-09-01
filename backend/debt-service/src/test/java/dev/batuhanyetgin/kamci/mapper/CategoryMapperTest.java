package dev.batuhanyetgin.kamci.mapper;

import dev.batuhanyetgin.kamci.document.CategoryDocument;
import dev.batuhanyetgin.kamci.document.embedded.MemberEmbedded;
import dev.batuhanyetgin.kamci.dto.response.GetAllCategoryResponse;
import dev.batuhanyetgin.kamci.enums.AccessLevelType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryMapperTest {

    private final CategoryMapper mapper = new CategoryMapper();

    @Test
    void convert_mapsTopLevelFields() {
        final CategoryDocument doc = CategoryDocument.builder()
                .ownerId("owner-1")
                .name("Bills")
                .members(List.of())
                .build();
        doc.setId("cat-1");

        final GetAllCategoryResponse response = mapper.convert(doc);

        assertThat(response.getId()).isEqualTo("cat-1");
        assertThat(response.getName()).isEqualTo("Bills");
        assertThat(response.getOwnerId()).isEqualTo("owner-1");
        assertThat(response.getMembers()).isEmpty();
    }

    @Test
    void convert_populatesMemberUserId() {
        // S2 (App Store blocker) regression guard: getAll must expose members[].userId
        // so the app can resolve who a shared category is shared with without leaking email only.
        final CategoryDocument doc = CategoryDocument.builder()
                .ownerId("owner-1")
                .name("Shared")
                .members(List.of(
                        MemberEmbedded.builder()
                                .userId("member-42")
                                .email("member@example.com")
                                .accessLevel(AccessLevelType.WRITE)
                                .build()))
                .build();
        doc.setId("cat-2");

        final GetAllCategoryResponse response = mapper.convert(doc);

        assertThat(response.getMembers()).hasSize(1);
        final GetAllCategoryResponse.MemberItem member = response.getMembers().get(0);
        assertThat(member.getUserId())
                .as("members[].userId must be non-null in getAll response")
                .isNotNull()
                .isEqualTo("member-42");
        assertThat(member.getEmail()).isEqualTo("member@example.com");
        assertThat(member.getAccessLevel()).isEqualTo(AccessLevelType.WRITE);
    }
}
