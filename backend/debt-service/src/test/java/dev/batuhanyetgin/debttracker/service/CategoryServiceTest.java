package dev.batuhanyetgin.debttracker.service;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import dev.batuhanyetgin.debttracker.document.CategoryDocument;
import dev.batuhanyetgin.debttracker.document.embedded.MemberEmbedded;
import dev.batuhanyetgin.debttracker.dto.request.AddMemberRequest;
import dev.batuhanyetgin.debttracker.dto.request.BulkAddMemberRequest;
import dev.batuhanyetgin.debttracker.dto.request.CreateCategoryRequest;
import dev.batuhanyetgin.debttracker.dto.request.UpdateCategoryRequest;
import dev.batuhanyetgin.debttracker.dto.response.GetAllCategoryResponse;
import dev.batuhanyetgin.debttracker.dto.response.SaveCategoryResponse;
import dev.batuhanyetgin.debttracker.dto.response.UpdateCategoryResponse;
import dev.batuhanyetgin.debttracker.enums.AccessLevelType;
import dev.batuhanyetgin.debttracker.client.AuthClient;
import dev.batuhanyetgin.debttracker.repositoryservice.CategoryRepositoryService;
import dev.batuhanyetgin.debttracker.repositoryservice.DebtRepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepositoryService categoryRS;
    @Mock private DebtRepositoryService debtRS;
    @Mock private AuthClient authClient;

    private final Executor taskExecutor = Runnable::run;

    private CategoryService categoryService;

    private static final String OWNER_ID  = "owner123";
    private static final String OTHER_ID  = "other456";
    private static final String MEMBER_ID = "member789";
    private static final String CAT_ID    = "cat123";
    private static final String CAT_ID_2  = "cat456";

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRS, debtRS, authClient, taskExecutor);
    }

    private CategoryDocument buildCategory(final String id, final String ownerId) {
        return CategoryDocument.builder()
                .id(id)
                .ownerId(ownerId)
                .name("Test Category")
                .build();
    }

    private CategoryDocument buildCategoryWithMember(final String id, final String ownerId,
                                                      final String memberId, final AccessLevelType level) {
        final List<MemberEmbedded> members = new ArrayList<>();
        members.add(MemberEmbedded.builder().userId(memberId).accessLevel(level).build());
        return CategoryDocument.builder()
                .id(id).ownerId(ownerId).name("Category")
                .members(members)
                .build();
    }

    // ─── save ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("under limit → saves and returns SaveCategoryResponse")
        void underLimit_savesAndReturns() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                when(categoryRS.countByOwnerId(OWNER_ID)).thenReturn(0L);
                final CategoryDocument saved = buildCategory(CAT_ID, OWNER_ID);
                when(categoryRS.save(any(CategoryDocument.class))).thenReturn(saved);

                final CreateCategoryRequest request = new CreateCategoryRequest();
                request.setName("Test Category");

                final SaveCategoryResponse response = categoryService.save(request);

                assertThat(response.getId()).isEqualTo(CAT_ID);
                assertThat(response.getName()).isEqualTo("Test Category");
            }
        }

        @Test
        @DisplayName("category limit exceeded → throws BAD_REQUEST")
        void limitExceeded_throwsBadRequest() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);
                when(categoryRS.countByOwnerId(OWNER_ID)).thenReturn(150L);

                final CreateCategoryRequest request = new CreateCategoryRequest();
                request.setName("New Category");

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> categoryService.save(request))
                        .matches(e -> e.getStatus() == HttpStatus.BAD_REQUEST);

                verify(categoryRS, never()).save(any());
            }
        }
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("owner updates category name → returns UpdateCategoryResponse")
        void ownerUpdates_returnsResponse() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final CategoryDocument cat = buildCategory(CAT_ID, OWNER_ID);
                when(categoryRS.findById(CAT_ID)).thenReturn(cat);
                final CategoryDocument updated = buildCategory(CAT_ID, OWNER_ID);
                updated.setName("New Name");
                when(categoryRS.save(cat)).thenReturn(updated);

                final UpdateCategoryRequest request = new UpdateCategoryRequest();
                request.setId(CAT_ID);
                request.setName("New Name");

                final UpdateCategoryResponse response = categoryService.update(request);

                assertThat(response.getId()).isEqualTo(CAT_ID);
                assertThat(response.getName()).isEqualTo("New Name");
            }
        }

        @Test
        @DisplayName("non-owner → throws FORBIDDEN")
        void nonOwner_throwsForbidden() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OTHER_ID);

                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final UpdateCategoryRequest request = new UpdateCategoryRequest();
                request.setId(CAT_ID);
                request.setName("New Name");

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> categoryService.update(request))
                        .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);

                verify(categoryRS, never()).save(any());
            }
        }
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("owner deletes → removes debts and category")
        void ownerDeletes_removesDebtsAndCategory() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                categoryService.delete(CAT_ID);

                verify(debtRS).deleteByCategoryId(CAT_ID);
                verify(categoryRS).delete(CAT_ID);
            }
        }

        @Test
        @DisplayName("non-owner → throws FORBIDDEN, nothing deleted")
        void nonOwner_throwsForbidden() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OTHER_ID);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> categoryService.delete(CAT_ID))
                        .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);

                verify(debtRS, never()).deleteByCategoryId(any());
                verify(categoryRS, never()).delete(any());
            }
        }
    }

    // ─── addMember ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addMember")
    class AddMember {

        @Test
        @DisplayName("owner adds new member → saves")
        void ownerAddsNewMember_saves() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final CategoryDocument cat = buildCategory(CAT_ID, OWNER_ID);
                when(categoryRS.findById(CAT_ID)).thenReturn(cat);
                when(authClient.userExists(MEMBER_ID)).thenReturn(true);
                when(categoryRS.save(cat)).thenReturn(cat);

                final AddMemberRequest request = new AddMemberRequest();
                request.setUserId(MEMBER_ID);
                request.setAccessLevel(AccessLevelType.READ);

                assertThatNoException().isThrownBy(() -> categoryService.addMember(CAT_ID, request));
                verify(categoryRS).save(cat);
            }
        }

        @Test
        @DisplayName("non-owner → throws FORBIDDEN")
        void nonOwner_throwsForbidden() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OTHER_ID);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final AddMemberRequest request = new AddMemberRequest();
                request.setUserId(MEMBER_ID);
                request.setAccessLevel(AccessLevelType.READ);

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> categoryService.addMember(CAT_ID, request))
                        .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);

                verify(categoryRS, never()).save(any());
            }
        }

        @Test
        @DisplayName("user does not exist in auth-service → throws NOT_FOUND")
        void userNotFound_throwsNotFound() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));
                when(authClient.userExists(MEMBER_ID)).thenReturn(false);

                final AddMemberRequest request = new AddMemberRequest();
                request.setUserId(MEMBER_ID);
                request.setAccessLevel(AccessLevelType.READ);

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> categoryService.addMember(CAT_ID, request))
                        .matches(e -> e.getStatus() == HttpStatus.NOT_FOUND);

                verify(categoryRS, never()).save(any());
            }
        }

        @Test
        @DisplayName("member already exists → throws CONFLICT")
        void alreadyMember_throwsConflict() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final CategoryDocument cat = buildCategoryWithMember(CAT_ID, OWNER_ID, MEMBER_ID, AccessLevelType.READ);
                when(categoryRS.findById(CAT_ID)).thenReturn(cat);
                when(authClient.userExists(MEMBER_ID)).thenReturn(true);

                final AddMemberRequest request = new AddMemberRequest();
                request.setUserId(MEMBER_ID);
                request.setAccessLevel(AccessLevelType.WRITE);

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> categoryService.addMember(CAT_ID, request))
                        .matches(e -> e.getStatus() == HttpStatus.CONFLICT);

                verify(categoryRS, never()).save(any());
            }
        }
    }

    // ─── bulkAddMember ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("bulkAddMember")
    class BulkAddMember {

        @Test
        @DisplayName("owner adds member to multiple categories → saves each")
        void ownerBulkAdds_savesEach() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));
                when(categoryRS.findById(CAT_ID_2)).thenReturn(buildCategory(CAT_ID_2, OWNER_ID));
                when(authClient.userExists(MEMBER_ID)).thenReturn(true);
                when(categoryRS.save(any())).thenAnswer(inv -> inv.getArgument(0));

                final BulkAddMemberRequest request = new BulkAddMemberRequest();
                request.setCategoryIds(List.of(CAT_ID, CAT_ID_2));
                request.setUserId(MEMBER_ID);
                request.setAccessLevel(AccessLevelType.READ);

                assertThatNoException().isThrownBy(() -> categoryService.bulkAddMember(request));
                verify(categoryRS, times(2)).save(any());
            }
        }

        @Test
        @DisplayName("one category not owned → throws FORBIDDEN")
        void oneNotOwned_throwsForbidden() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));
                when(categoryRS.findById(CAT_ID_2)).thenReturn(buildCategory(CAT_ID_2, OTHER_ID));
                when(authClient.userExists(MEMBER_ID)).thenReturn(true);
                when(categoryRS.save(any())).thenAnswer(inv -> inv.getArgument(0));

                final BulkAddMemberRequest request = new BulkAddMemberRequest();
                request.setCategoryIds(List.of(CAT_ID, CAT_ID_2));
                request.setUserId(MEMBER_ID);
                request.setAccessLevel(AccessLevelType.READ);

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> categoryService.bulkAddMember(request))
                        .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);
            }
        }
    }

    // ─── removeMember ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("removeMember")
    class RemoveMember {

        @Test
        @DisplayName("owner removes member → member no longer in list")
        void ownerRemovesMember_memberGone() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final CategoryDocument cat = buildCategoryWithMember(CAT_ID, OWNER_ID, MEMBER_ID, AccessLevelType.READ);
                when(categoryRS.findById(CAT_ID)).thenReturn(cat);
                when(categoryRS.save(cat)).thenReturn(cat);

                categoryService.removeMember(CAT_ID, MEMBER_ID);

                assertThat(cat.getMembers()).noneMatch(m -> m.getUserId().equals(MEMBER_ID));
                verify(categoryRS).save(cat);
            }
        }

        @Test
        @DisplayName("non-owner → throws FORBIDDEN")
        void nonOwner_throwsForbidden() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OTHER_ID);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> categoryService.removeMember(CAT_ID, MEMBER_ID))
                        .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);

                verify(categoryRS, never()).save(any());
            }
        }
    }

    // ─── getAll ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAll")
    class GetAll {

        @Test
        @DisplayName("returns owned and shared categories")
        void getAll_returnsOwnedAndShared() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                when(categoryRS.findByOwnerId(OWNER_ID)).thenReturn(List.of(buildCategory(CAT_ID, OWNER_ID)));
                when(categoryRS.findByMemberUserId(OWNER_ID)).thenReturn(List.of(buildCategory(CAT_ID_2, OTHER_ID)));

                final List<GetAllCategoryResponse> result = categoryService.getAll();

                assertThat(result).hasSize(2);
                assertThat(result).extracting(GetAllCategoryResponse::getId)
                        .containsExactlyInAnyOrder(CAT_ID, CAT_ID_2);
            }
        }

        @Test
        @DisplayName("no categories → returns empty list")
        void getAll_noCategories_returnsEmpty() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                when(categoryRS.findByOwnerId(OWNER_ID)).thenReturn(List.of());
                when(categoryRS.findByMemberUserId(OWNER_ID)).thenReturn(List.of());

                assertThat(categoryService.getAll()).isEmpty();
            }
        }
    }
}
