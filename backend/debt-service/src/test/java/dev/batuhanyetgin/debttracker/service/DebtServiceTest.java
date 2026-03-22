package dev.batuhanyetgin.debttracker.service;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import dev.batuhanyetgin.debttracker.document.CategoryDocument;
import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.document.embedded.MemberEmbedded;
import dev.batuhanyetgin.debttracker.dto.request.CreateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.request.UpdateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.response.DebtItemResponse;
import dev.batuhanyetgin.debttracker.dto.response.DebtResponse;
import dev.batuhanyetgin.debttracker.dto.response.GetDebtResponse;
import dev.batuhanyetgin.debttracker.enums.AccessLevelType;
import dev.batuhanyetgin.debttracker.enums.PaymentPeriodType;
import dev.batuhanyetgin.debttracker.mapper.DebtMapper;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebtServiceTest {

    @Mock private DebtRepositoryService debtRS;
    @Mock private CategoryRepositoryService categoryRS;
    @Mock private DebtMapper debtMapper;
    @Mock private DebtEventAsyncPublisher debtEventAsyncPublisher;

    // Synchronous executor — runs tasks on the calling thread
    private final Executor taskExecutor = Runnable::run;

    private DebtService debtService;

    private static final String OWNER_ID  = "owner123";
    private static final String OTHER_ID  = "other456";
    private static final String CAT_ID    = "cat123";
    private static final String NEW_CAT_ID = "cat456";
    private static final String DEBT_ID   = "debt789";

    @BeforeEach
    void setUp() {
        debtService = new DebtService(debtRS, categoryRS, debtMapper, debtEventAsyncPublisher, taskExecutor);
    }

    private DebtDocument buildDebt(final String id, final String ownerId, final String catId) {
        return DebtDocument.builder()
                .id(id)
                .name("Test Debt")
                .ownerId(ownerId)
                .categoryId(catId)
                .amount(BigDecimal.TEN)
                .paymentPeriodType(PaymentPeriodType.MONTHLY)
                .startDate(LocalDateTime.now())
                .build();
    }

    private CategoryDocument buildCategory(final String id, final String ownerId) {
        return CategoryDocument.builder()
                .id(id)
                .ownerId(ownerId)
                .name("Test Category")
                .build();
    }

    private CategoryDocument buildCategoryWithWriteMember(final String id, final String ownerId, final String memberId) {
        return CategoryDocument.builder()
                .id(id)
                .ownerId(ownerId)
                .name("Shared Category")
                .members(List.of(MemberEmbedded.builder()
                        .userId(memberId)
                        .accessLevel(AccessLevelType.WRITE)
                        .build()))
                .build();
    }

    private CategoryDocument buildCategoryWithReadMember(final String id, final String ownerId, final String memberId) {
        return CategoryDocument.builder()
                .id(id)
                .ownerId(ownerId)
                .name("Shared Category")
                .members(List.of(MemberEmbedded.builder()
                        .userId(memberId)
                        .accessLevel(AccessLevelType.READ)
                        .build()))
                .build();
    }

    private DebtItemResponse buildDebtItemResponse() {
        return DebtItemResponse.builder()
                .id(DEBT_ID)
                .name("Test Debt")
                .amount(BigDecimal.TEN)
                .build();
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("owner creates debt in own category → saves and returns response")
        void ownerCreatesDebt_returnsResponse() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                when(debtRS.countByOwnerId(OWNER_ID)).thenReturn(0L);
                final CategoryDocument cat = buildCategory(CAT_ID, OWNER_ID);
                when(categoryRS.findById(CAT_ID)).thenReturn(cat);

                final DebtDocument saved = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtMapper.convert(any(CreateDebtRequest.class))).thenReturn(saved);
                when(debtRS.save(saved)).thenReturn(saved);
                final DebtItemResponse item = buildDebtItemResponse();
                when(debtMapper.convert(saved)).thenReturn(item);

                final CreateDebtRequest request = CreateDebtRequest.builder()
                        .name("Test Debt").categoryId(CAT_ID)
                        .amount(BigDecimal.TEN).paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .startDate(LocalDateTime.now()).build();

                final DebtResponse response = debtService.create(request);

                assertThat(response.getDebt()).isEqualTo(item);
                verify(debtRS).save(saved);
                verify(debtEventAsyncPublisher).publishDebtAddedEvent(any(), any(), eq(OWNER_ID), eq("en"));
            }
        }

        @Test
        @DisplayName("write-level member creates debt in shared category → succeeds")
        void writeMemberCreatesDebt_succeeds() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OTHER_ID);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                when(debtRS.countByOwnerId(OTHER_ID)).thenReturn(0L);
                final CategoryDocument cat = buildCategoryWithWriteMember(CAT_ID, OWNER_ID, OTHER_ID);
                when(categoryRS.findById(CAT_ID)).thenReturn(cat);

                final DebtDocument saved = buildDebt(DEBT_ID, OTHER_ID, CAT_ID);
                when(debtMapper.convert(any(CreateDebtRequest.class))).thenReturn(saved);
                when(debtRS.save(saved)).thenReturn(saved);
                when(debtMapper.convert(saved)).thenReturn(buildDebtItemResponse());

                final CreateDebtRequest request = CreateDebtRequest.builder()
                        .name("Test Debt").categoryId(CAT_ID)
                        .amount(BigDecimal.TEN).paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .startDate(LocalDateTime.now()).build();

                assertThatNoException().isThrownBy(() -> debtService.create(request));
            }
        }

        @Test
        @DisplayName("debt limit exceeded → throws BAD_REQUEST")
        void limitExceeded_throwsBadRequest() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                when(debtRS.countByOwnerId(OWNER_ID)).thenReturn(150L);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final CreateDebtRequest request = CreateDebtRequest.builder()
                        .name("Test Debt").categoryId(CAT_ID)
                        .amount(BigDecimal.TEN).paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .startDate(LocalDateTime.now()).build();

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> debtService.create(request))
                        .matches(e -> e.getStatus() == HttpStatus.BAD_REQUEST);

                verify(debtRS, never()).save(any());
            }
        }

        @Test
        @DisplayName("read-only member has no write access → throws FORBIDDEN")
        void readOnlyMember_throwsForbidden() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OTHER_ID);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                when(debtRS.countByOwnerId(OTHER_ID)).thenReturn(0L);
                final CategoryDocument cat = buildCategoryWithReadMember(CAT_ID, OWNER_ID, OTHER_ID);
                when(categoryRS.findById(CAT_ID)).thenReturn(cat);

                final CreateDebtRequest request = CreateDebtRequest.builder()
                        .name("Test Debt").categoryId(CAT_ID)
                        .amount(BigDecimal.TEN).paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .startDate(LocalDateTime.now()).build();

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> debtService.create(request))
                        .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);
            }
        }

        @Test
        @DisplayName("non-member has no write access → throws FORBIDDEN")
        void nonMember_throwsForbidden() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OTHER_ID);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                when(debtRS.countByOwnerId(OTHER_ID)).thenReturn(0L);
                // Category owned by OWNER_ID, OTHER_ID not a member
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final CreateDebtRequest request = CreateDebtRequest.builder()
                        .name("Test Debt").categoryId(CAT_ID)
                        .amount(BigDecimal.TEN).paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .startDate(LocalDateTime.now()).build();

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> debtService.create(request))
                        .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);
            }
        }
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("owner updates debt in same category → succeeds")
        void ownerUpdatesSameCategory_succeeds() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final DebtDocument updated = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtMapper.convert(any(UpdateDebtRequest.class))).thenReturn(updated);
                when(debtRS.update(updated)).thenReturn(updated);
                when(debtMapper.convert(updated)).thenReturn(buildDebtItemResponse());

                final UpdateDebtRequest request = UpdateDebtRequest.builder()
                        .id(DEBT_ID).name("Updated").categoryId(CAT_ID)
                        .amount(BigDecimal.TEN).paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .startDate(LocalDateTime.now()).build();

                final DebtResponse response = debtService.update(request);
                assertThat(response.getDebt()).isNotNull();
                verify(debtRS).update(updated);
            }
        }

        @Test
        @DisplayName("owner moves debt to new category with write access → succeeds")
        void ownerMovesToNewCategory_succeeds() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));
                when(categoryRS.findById(NEW_CAT_ID)).thenReturn(buildCategory(NEW_CAT_ID, OWNER_ID));

                final DebtDocument updated = buildDebt(DEBT_ID, OWNER_ID, NEW_CAT_ID);
                when(debtMapper.convert(any(UpdateDebtRequest.class))).thenReturn(updated);
                when(debtRS.update(updated)).thenReturn(updated);
                when(debtMapper.convert(updated)).thenReturn(buildDebtItemResponse());

                final UpdateDebtRequest request = UpdateDebtRequest.builder()
                        .id(DEBT_ID).name("Updated").categoryId(NEW_CAT_ID)
                        .amount(BigDecimal.TEN).paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .startDate(LocalDateTime.now()).build();

                assertThatNoException().isThrownBy(() -> debtService.update(request));
            }
        }

        @Test
        @DisplayName("no write access to current category → throws FORBIDDEN")
        void noAccessToCurrentCategory_throwsForbidden() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OTHER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final UpdateDebtRequest request = UpdateDebtRequest.builder()
                        .id(DEBT_ID).name("Updated").categoryId(CAT_ID)
                        .amount(BigDecimal.TEN).paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .startDate(LocalDateTime.now()).build();

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> debtService.update(request))
                        .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);
            }
        }

        @Test
        @DisplayName("no write access to new category → throws FORBIDDEN")
        void noAccessToNewCategory_throwsForbidden() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));
                // NEW_CAT_ID owned by someone else
                when(categoryRS.findById(NEW_CAT_ID)).thenReturn(buildCategory(NEW_CAT_ID, OTHER_ID));

                final UpdateDebtRequest request = UpdateDebtRequest.builder()
                        .id(DEBT_ID).name("Updated").categoryId(NEW_CAT_ID)
                        .amount(BigDecimal.TEN).paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .startDate(LocalDateTime.now()).build();

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> debtService.update(request))
                        .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);
            }
        }
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("owner deletes own debt → calls debtRS.delete")
        void ownerDeletesDebt_succeeds() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                when(debtRS.findById(DEBT_ID)).thenReturn(buildDebt(DEBT_ID, OWNER_ID, CAT_ID));
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                debtService.delete(DEBT_ID);
                verify(debtRS).delete(DEBT_ID);
            }
        }

        @Test
        @DisplayName("non-owner with no write access → throws FORBIDDEN")
        void noAccess_throwsForbidden() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OTHER_ID);

                when(debtRS.findById(DEBT_ID)).thenReturn(buildDebt(DEBT_ID, OWNER_ID, CAT_ID));
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> debtService.delete(DEBT_ID))
                        .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);

                verify(debtRS, never()).delete(any());
            }
        }
    }

    // ─── get ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("get")
    class Get {

        @Test
        @DisplayName("returns owned and shared categories with grouped debts")
        void get_returnsGroupedDebts() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final CategoryDocument owned = buildCategory(CAT_ID, OWNER_ID);
                final CategoryDocument shared = buildCategory(NEW_CAT_ID, OTHER_ID);
                when(categoryRS.findByOwnerId(OWNER_ID)).thenReturn(List.of(owned));
                when(categoryRS.findByMemberUserId(OWNER_ID)).thenReturn(List.of(shared));

                final DebtDocument debt = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtRS.findByCategoryIdIn(List.of(CAT_ID, NEW_CAT_ID))).thenReturn(List.of(debt));
                when(debtMapper.convert(debt)).thenReturn(buildDebtItemResponse());

                final GetDebtResponse response = debtService.get();

                assertThat(response.getCategories()).hasSize(2);
                assertThat(response.getCategories()).anySatisfy(c ->
                        assertThat(c.getCategoryId()).isEqualTo(CAT_ID));
                assertThat(response.getCategories()).anySatisfy(c ->
                        assertThat(c.getCategoryId()).isEqualTo(NEW_CAT_ID));
            }
        }

        @Test
        @DisplayName("owned flag is set correctly")
        void get_ownedFlagSetCorrectly() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final CategoryDocument owned = buildCategory(CAT_ID, OWNER_ID);
                final CategoryDocument shared = buildCategory(NEW_CAT_ID, OTHER_ID);
                when(categoryRS.findByOwnerId(OWNER_ID)).thenReturn(List.of(owned));
                when(categoryRS.findByMemberUserId(OWNER_ID)).thenReturn(List.of(shared));
                when(debtRS.findByCategoryIdIn(anyList())).thenReturn(List.of());

                final GetDebtResponse response = debtService.get();

                response.getCategories().forEach(c -> {
                    if (CAT_ID.equals(c.getCategoryId())) {
                        assertThat(c.isOwned()).isTrue();
                        assertThat(c.getOwnerUserId()).isNull();
                    } else {
                        assertThat(c.isOwned()).isFalse();
                        assertThat(c.getOwnerUserId()).isEqualTo(OTHER_ID);
                    }
                });
            }
        }

        @Test
        @DisplayName("no categories → returns empty list")
        void get_noCategories_returnsEmpty() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                when(categoryRS.findByOwnerId(OWNER_ID)).thenReturn(List.of());
                when(categoryRS.findByMemberUserId(OWNER_ID)).thenReturn(List.of());
                when(debtRS.findByCategoryIdIn(List.of())).thenReturn(List.of());

                final GetDebtResponse response = debtService.get();
                assertThat(response.getCategories()).isEmpty();
            }
        }
    }
}
