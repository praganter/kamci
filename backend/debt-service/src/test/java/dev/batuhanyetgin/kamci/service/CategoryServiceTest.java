package dev.batuhanyetgin.kamci.service;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import dev.batuhanyetgin.kamci.document.CategoryDocument;
import dev.batuhanyetgin.kamci.document.embedded.MemberEmbedded;
import dev.batuhanyetgin.kamci.dto.request.AddMemberRequest;
import dev.batuhanyetgin.kamci.dto.request.BulkAddMemberRequest;
import dev.batuhanyetgin.kamci.dto.request.CreateCategoryRequest;
import dev.batuhanyetgin.kamci.dto.request.UpdateCategoryRequest;
import dev.batuhanyetgin.kamci.document.DebtDocument;
import dev.batuhanyetgin.kamci.dto.response.GetAllCategoryResponse;
import dev.batuhanyetgin.kamci.dto.response.MigrateCategoryResponse;
import dev.batuhanyetgin.kamci.dto.response.SaveCategoryResponse;
import dev.batuhanyetgin.kamci.dto.response.UpdateCategoryResponse;
import dev.batuhanyetgin.kamci.enums.AccessLevelType;
import dev.batuhanyetgin.kamci.client.AuthClient;
import dev.batuhanyetgin.kamci.producer.DebtEventProducer;
import dev.batuhanyetgin.kamci.mapper.CategoryMapper;
import dev.batuhanyetgin.kamci.repositoryservice.CategoryRepositoryService;
import dev.batuhanyetgin.kamci.repositoryservice.DebtRepositoryService;
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

import feign.FeignException;
import feign.Request;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepositoryService categoryRS;
    @Mock private DebtRepositoryService debtRS;
    @Mock private AuthClient authClient;
    @Mock private DebtEventProducer eventProducer;
    @Mock private CategoryMapper categoryMapper;

    private final Executor taskExecutor = Runnable::run;

    private CategoryService categoryService;

    private static final String OWNER_ID     = "owner123";
    private static final String OTHER_ID     = "other456";
    private static final String MEMBER_ID    = "member789";
    private static final String MEMBER_EMAIL = "member@example.com";
    private static final String CAT_ID       = "cat123";
    private static final String CAT_ID_2     = "cat456";

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRS, debtRS, authClient, eventProducer, categoryMapper, taskExecutor);
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
        members.add(MemberEmbedded.builder()
                .userId(memberId)
                .email(MEMBER_EMAIL)
                .accessLevel(level)
                .build());
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
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);

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
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                when(categoryRS.countByOwnerId(OWNER_ID)).thenReturn(200L);

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
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);

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
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);

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
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
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
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> categoryService.delete(CAT_ID))
                        .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);

                verify(debtRS, never()).deleteByCategoryId(any());
                verify(categoryRS, never()).delete(any());
            }
        }
    }

    // ─── deleteAndMigrate ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteAndMigrate")
    class DeleteAndMigrate {

        private static final String TARGET_ID = "catTarget";

        private DebtDocument buildDebt(final String id, final String categoryId) {
            return DebtDocument.builder()
                    .categoryId(categoryId)
                    .build();
        }

        @Test
        @DisplayName("owner migrates debts → debts updated, source category deleted, count returned")
        void ownerMigrates_debtsUpdatedAndCategoryDeleted() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));
                when(categoryRS.findById(TARGET_ID)).thenReturn(buildCategory(TARGET_ID, OWNER_ID));
                final List<DebtDocument> debts = List.of(buildDebt("d1", CAT_ID), buildDebt("d2", CAT_ID));
                when(debtRS.findByCategoryId(CAT_ID)).thenReturn(debts);

                final MigrateCategoryResponse response = categoryService.deleteAndMigrate(CAT_ID, TARGET_ID);

                assertThat(response.getMigratedDebtCount()).isEqualTo(2);
                verify(debtRS, times(2)).update(any(DebtDocument.class));
                verify(categoryRS).delete(CAT_ID);
            }
        }

        @Test
        @DisplayName("category has no debts → update never called, source category still deleted, count is 0")
        void noDebts_updateNeverCalledCategoryDeleted() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));
                when(categoryRS.findById(TARGET_ID)).thenReturn(buildCategory(TARGET_ID, OWNER_ID));
                when(debtRS.findByCategoryId(CAT_ID)).thenReturn(List.of());

                final MigrateCategoryResponse response = categoryService.deleteAndMigrate(CAT_ID, TARGET_ID);

                assertThat(response.getMigratedDebtCount()).isEqualTo(0);
                verify(debtRS, never()).update(any());
                verify(categoryRS).delete(CAT_ID);
            }
        }

        @Test
        @DisplayName("source equals target → throws BAD_REQUEST, no DB calls")
        void sameId_throwsBadRequest() {
            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> categoryService.deleteAndMigrate(CAT_ID, CAT_ID))
                    .matches(e -> e.getStatus() == HttpStatus.BAD_REQUEST);

            verify(categoryRS, never()).findById(any());
            verify(debtRS, never()).findByCategoryId(any());
        }

        @Test
        @DisplayName("non-owner of source → throws FORBIDDEN, target never queried")
        void nonOwnerSource_throwsForbidden() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OTHER_ID);

                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> categoryService.deleteAndMigrate(CAT_ID, TARGET_ID))
                        .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);

                verify(categoryRS, never()).findById(TARGET_ID);
                verify(debtRS, never()).findByCategoryId(any());
            }
        }

        @Test
        @DisplayName("non-owner of target → throws FORBIDDEN, debts never fetched")
        void nonOwnerTarget_throwsForbidden() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));
                when(categoryRS.findById(TARGET_ID)).thenReturn(buildCategory(TARGET_ID, OTHER_ID));

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> categoryService.deleteAndMigrate(CAT_ID, TARGET_ID))
                        .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);

                verify(debtRS, never()).findByCategoryId(any());
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
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);

                final CategoryDocument cat = buildCategory(CAT_ID, OWNER_ID);
                when(categoryRS.findById(CAT_ID)).thenReturn(cat);
                when(authClient.getUserIdByEmail(MEMBER_EMAIL)).thenReturn(MEMBER_ID);
                when(categoryRS.save(cat)).thenReturn(cat);

                final AddMemberRequest request = new AddMemberRequest();
                request.setEmail(MEMBER_EMAIL);
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
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final AddMemberRequest request = new AddMemberRequest();
                request.setEmail(MEMBER_EMAIL);
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
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);

                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));
                final Request feignReq = Request.create(
                        Request.HttpMethod.GET, "http://auth-service/auth/user/id-by-email/" + MEMBER_EMAIL,
                        Collections.emptyMap(), null, null, null);
                when(authClient.getUserIdByEmail(MEMBER_EMAIL))
                        .thenThrow(new FeignException.NotFound("not found", feignReq, null, null));

                final AddMemberRequest request = new AddMemberRequest();
                request.setEmail(MEMBER_EMAIL);
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
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);

                final CategoryDocument cat = buildCategoryWithMember(CAT_ID, OWNER_ID, MEMBER_ID, AccessLevelType.READ);
                when(categoryRS.findById(CAT_ID)).thenReturn(cat);
                when(authClient.getUserIdByEmail(MEMBER_EMAIL)).thenReturn(MEMBER_ID);

                final AddMemberRequest request = new AddMemberRequest();
                request.setEmail(MEMBER_EMAIL);
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
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);

                final CategoryDocument cat1 = buildCategory(CAT_ID, OWNER_ID);
                final CategoryDocument cat2 = buildCategory(CAT_ID_2, OWNER_ID);
                when(categoryRS.findByIdIn(List.of(CAT_ID, CAT_ID_2))).thenReturn(List.of(cat1, cat2));
                when(authClient.getUserIdByEmail(MEMBER_EMAIL)).thenReturn(MEMBER_ID);
                when(categoryRS.save(any())).thenAnswer(inv -> inv.getArgument(0));

                final BulkAddMemberRequest request = new BulkAddMemberRequest();
                request.setCategoryIds(List.of(CAT_ID, CAT_ID_2));
                request.setEmail(MEMBER_EMAIL);
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
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);

                final CategoryDocument cat1 = buildCategory(CAT_ID, OWNER_ID);
                final CategoryDocument cat2 = buildCategory(CAT_ID_2, OTHER_ID);
                when(categoryRS.findByIdIn(List.of(CAT_ID, CAT_ID_2))).thenReturn(List.of(cat1, cat2));
                when(authClient.getUserIdByEmail(MEMBER_EMAIL)).thenReturn(MEMBER_ID);

                final BulkAddMemberRequest request = new BulkAddMemberRequest();
                request.setCategoryIds(List.of(CAT_ID, CAT_ID_2));
                request.setEmail(MEMBER_EMAIL);
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
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);

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
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
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
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);

                when(categoryRS.findByOwnerId(OWNER_ID)).thenReturn(List.of(buildCategory(CAT_ID, OWNER_ID)));
                when(categoryRS.findByMemberUserId(OWNER_ID)).thenReturn(List.of(buildCategory(CAT_ID_2, OTHER_ID)));
                when(categoryMapper.convert(any(CategoryDocument.class))).thenAnswer(inv -> {
                    final CategoryDocument doc = inv.getArgument(0);
                    return GetAllCategoryResponse.builder().id(doc.getId()).name(doc.getName()).ownerId(doc.getOwnerId()).members(List.of()).build();
                });

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
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);

                when(categoryRS.findByOwnerId(OWNER_ID)).thenReturn(List.of());
                when(categoryRS.findByMemberUserId(OWNER_ID)).thenReturn(List.of());

                assertThat(categoryService.getAll()).isEmpty();
            }
        }
    }
}
