package dev.batuhanyetgin.kamci.service;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import dev.batuhanyetgin.kamci.document.CategoryDocument;
import dev.batuhanyetgin.kamci.document.DebtDocument;
import dev.batuhanyetgin.kamci.document.embedded.MemberEmbedded;
import dev.batuhanyetgin.kamci.document.embedded.PaymentEmbedded;
import dev.batuhanyetgin.kamci.dto.request.CreateDebtRequest;
import dev.batuhanyetgin.kamci.dto.request.PayDebtRequest;
import dev.batuhanyetgin.kamci.dto.request.SetPaidInstallmentsRequest;
import dev.batuhanyetgin.kamci.dto.request.SkipDebtRequest;
import dev.batuhanyetgin.kamci.dto.request.UpdateDebtRequest;
import dev.batuhanyetgin.kamci.dto.response.DebtDetailResponse;
import dev.batuhanyetgin.kamci.dto.response.DebtItemResponse;
import dev.batuhanyetgin.kamci.dto.response.DebtResponse;
import dev.batuhanyetgin.kamci.dto.response.GetDebtResponse;
import dev.batuhanyetgin.kamci.dto.response.MonthlySummaryResponse;
import dev.batuhanyetgin.kamci.dto.response.PaymentHistoryResponse;
import dev.batuhanyetgin.kamci.enums.AccessLevelType;
import dev.batuhanyetgin.kamci.enums.DebtStatus;
import dev.batuhanyetgin.kamci.enums.PaymentStatus;
import dev.batuhanyetgin.kamci.enums.PaymentPeriodType;
import dev.batuhanyetgin.kamci.mapper.DebtMapper;
import dev.batuhanyetgin.kamci.repositoryservice.CategoryRepositoryService;
import dev.batuhanyetgin.kamci.repositoryservice.DebtRepositoryService;
import dev.batuhanyetgin.kamci.repositoryservice.UserIncomeRepositoryService;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    @Mock private UserIncomeRepositoryService incomeRS;
    @Mock private LockProvider lockProvider;
    @Mock private SimpleLock simpleLock;

    // Synchronous executor — runs tasks on the calling thread
    private final Executor taskExecutor = Runnable::run;

    private DebtService debtService;

    private static final String OWNER_ID  = "owner123";
    private static final String OTHER_ID  = "other456";
    private static final String CAT_ID    = "cat123";
    private static final String NEW_CAT_ID = "cat456";
    private static final String DEBT_ID   = "debt789";

    @BeforeEach
    void setUp() throws Throwable {
        lenient().when(lockProvider.lock(any())).thenReturn(java.util.Optional.of(simpleLock));
        final LockingTaskExecutor lockingTaskExecutor = new DefaultLockingTaskExecutor(lockProvider);
        debtService = new DebtService(debtRS, categoryRS, debtMapper, debtEventAsyncPublisher, incomeRS, lockingTaskExecutor, taskExecutor);
    }

    private DebtDocument buildDebt(final String id, final String ownerId, final String catId) {
        return DebtDocument.builder()
                .id(id)
                .name("Test Debt")
                .ownerId(ownerId)
                .categoryId(catId)
                .amount(BigDecimal.TEN)
                .paymentPeriodType(PaymentPeriodType.MONTHLY)
                .firstPaymentDate(LocalDateTime.now())
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

    private CategoryDocument buildCategoryWithEditMember(final String id, final String ownerId, final String memberId) {
        return CategoryDocument.builder()
                .id(id)
                .ownerId(ownerId)
                .name("Shared Category")
                .members(List.of(MemberEmbedded.builder()
                        .userId(memberId)
                        .accessLevel(AccessLevelType.EDIT)
                        .build()))
                .build();
    }

    private CategoryDocument buildCategoryWithDeleteMember(final String id, final String ownerId, final String memberId) {
        return CategoryDocument.builder()
                .id(id)
                .ownerId(ownerId)
                .name("Shared Category")
                .members(List.of(MemberEmbedded.builder()
                        .userId(memberId)
                        .accessLevel(AccessLevelType.DELETE)
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
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                final CategoryDocument cat = buildCategory(CAT_ID, OWNER_ID);
                when(categoryRS.findByOwnerId(OWNER_ID)).thenReturn(List.of(cat));
                when(debtRS.countByOwnerIdAndCategoryIdIn(eq(OWNER_ID), anyList())).thenReturn(0L);
                when(categoryRS.findById(CAT_ID)).thenReturn(cat);

                final DebtDocument saved = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtMapper.convert(any(CreateDebtRequest.class))).thenReturn(saved);
                when(debtRS.save(saved)).thenReturn(saved);
                final DebtItemResponse item = buildDebtItemResponse();
                when(debtMapper.convert(saved)).thenReturn(item);

                final CreateDebtRequest request = CreateDebtRequest.builder()
                        .name("Test Debt").categoryId(CAT_ID)
                        .amount(BigDecimal.TEN).paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .firstPaymentDate(LocalDateTime.now()).build();

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
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                final CategoryDocument cat = buildCategoryWithWriteMember(CAT_ID, OWNER_ID, OTHER_ID);
                when(categoryRS.findById(CAT_ID)).thenReturn(cat);

                final DebtDocument saved = buildDebt(DEBT_ID, OTHER_ID, CAT_ID);
                when(debtMapper.convert(any(CreateDebtRequest.class))).thenReturn(saved);
                when(debtRS.save(saved)).thenReturn(saved);
                when(debtMapper.convert(saved)).thenReturn(buildDebtItemResponse());

                final CreateDebtRequest request = CreateDebtRequest.builder()
                        .name("Test Debt").categoryId(CAT_ID)
                        .amount(BigDecimal.TEN).paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .firstPaymentDate(LocalDateTime.now()).build();

                assertThatNoException().isThrownBy(() -> debtService.create(request));
            }
        }

        @Test
        @DisplayName("debt limit exceeded → throws BAD_REQUEST")
        void limitExceeded_throwsBadRequest() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                final CategoryDocument cat = buildCategory(CAT_ID, OWNER_ID);
                when(categoryRS.findByOwnerId(OWNER_ID)).thenReturn(List.of(cat));
                when(debtRS.countByOwnerIdAndCategoryIdIn(eq(OWNER_ID), anyList())).thenReturn(500L);
                when(categoryRS.findById(CAT_ID)).thenReturn(cat);
                when(debtMapper.convert(any(CreateDebtRequest.class))).thenReturn(buildDebt(DEBT_ID, OWNER_ID, CAT_ID));

                final CreateDebtRequest request = CreateDebtRequest.builder()
                        .name("Test Debt").categoryId(CAT_ID)
                        .amount(BigDecimal.TEN).paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .firstPaymentDate(LocalDateTime.now()).build();

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
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                final CategoryDocument cat = buildCategoryWithReadMember(CAT_ID, OWNER_ID, OTHER_ID);
                when(categoryRS.findById(CAT_ID)).thenReturn(cat);

                final CreateDebtRequest request = CreateDebtRequest.builder()
                        .name("Test Debt").categoryId(CAT_ID)
                        .amount(BigDecimal.TEN).paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .firstPaymentDate(LocalDateTime.now()).build();

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
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                // Category owned by OWNER_ID, OTHER_ID not a member
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final CreateDebtRequest request = CreateDebtRequest.builder()
                        .name("Test Debt").categoryId(CAT_ID)
                        .amount(BigDecimal.TEN).paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .firstPaymentDate(LocalDateTime.now()).build();

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
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

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
                        .firstPaymentDate(LocalDateTime.now()).build();

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
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

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
                        .firstPaymentDate(LocalDateTime.now()).build();

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
                        .firstPaymentDate(LocalDateTime.now()).build();

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> debtService.update(request))
                        .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);
            }
        }

        @Test
        @DisplayName("existing payments are preserved after update")
        void existingPayments_preserved() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                final List<PaymentEmbedded> existingPayments = List.of(
                        PaymentEmbedded.builder().paid(true).paidTime(LocalDateTime.now()).build());
                final DebtDocument existing = DebtDocument.builder()
                        .id(DEBT_ID).ownerId(OWNER_ID).categoryId(CAT_ID)
                        .name("Test Debt").amount(BigDecimal.TEN)
                        .paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .firstPaymentDate(LocalDateTime.now())
                        .payments(existingPayments)
                        .build();
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final DebtDocument converted = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtMapper.convert(any(UpdateDebtRequest.class))).thenReturn(converted);
                when(debtRS.update(converted)).thenReturn(converted);
                when(debtMapper.convert(converted)).thenReturn(buildDebtItemResponse());

                final UpdateDebtRequest request = UpdateDebtRequest.builder()
                        .id(DEBT_ID).name("Updated").categoryId(CAT_ID)
                        .amount(BigDecimal.TEN).paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .firstPaymentDate(LocalDateTime.now()).build();

                debtService.update(request);

                assertThat(converted.getPayments()).isEqualTo(existingPayments);
            }
        }

        @Test
        @DisplayName("no write access to new category → throws FORBIDDEN")
        void noAccessToNewCategory_throwsForbidden() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));
                // NEW_CAT_ID owned by someone else
                when(categoryRS.findById(NEW_CAT_ID)).thenReturn(buildCategory(NEW_CAT_ID, OTHER_ID));

                final UpdateDebtRequest request = UpdateDebtRequest.builder()
                        .id(DEBT_ID).name("Updated").categoryId(NEW_CAT_ID)
                        .amount(BigDecimal.TEN).paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .firstPaymentDate(LocalDateTime.now()).build();

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> debtService.update(request))
                        .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);
            }
        }

        @Test
        @DisplayName("write-only member (no edit access) → throws FORBIDDEN")
        void writeOnlyMember_throwsForbidden() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OTHER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategoryWithWriteMember(CAT_ID, OWNER_ID, OTHER_ID));

                final UpdateDebtRequest request = UpdateDebtRequest.builder()
                        .id(DEBT_ID).name("Updated").categoryId(CAT_ID)
                        .amount(BigDecimal.TEN).paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .firstPaymentDate(LocalDateTime.now()).build();

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> debtService.update(request))
                        .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);
            }
        }

        @Test
        @DisplayName("edit-level member updates debt in shared category → succeeds")
        void editMember_succeeds() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OTHER_ID);
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategoryWithEditMember(CAT_ID, OWNER_ID, OTHER_ID));

                final DebtDocument updated = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtMapper.convert(any(UpdateDebtRequest.class))).thenReturn(updated);
                when(debtRS.update(updated)).thenReturn(updated);
                when(debtMapper.convert(updated)).thenReturn(buildDebtItemResponse());

                final UpdateDebtRequest request = UpdateDebtRequest.builder()
                        .id(DEBT_ID).name("Updated").categoryId(CAT_ID)
                        .amount(BigDecimal.TEN).paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .firstPaymentDate(LocalDateTime.now()).build();

                assertThatNoException().isThrownBy(() -> debtService.update(request));
                verify(debtRS).update(updated);
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
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

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

        @Test
        @DisplayName("edit-only member (no delete access) → throws FORBIDDEN")
        void editOnlyMember_throwsForbidden() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OTHER_ID);

                when(debtRS.findById(DEBT_ID)).thenReturn(buildDebt(DEBT_ID, OWNER_ID, CAT_ID));
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategoryWithEditMember(CAT_ID, OWNER_ID, OTHER_ID));

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> debtService.delete(DEBT_ID))
                        .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);

                verify(debtRS, never()).delete(any());
            }
        }

        @Test
        @DisplayName("delete-level member deletes debt → calls debtRS.delete")
        void deleteMember_succeeds() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OTHER_ID);

                when(debtRS.findById(DEBT_ID)).thenReturn(buildDebt(DEBT_ID, OWNER_ID, CAT_ID));
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategoryWithDeleteMember(CAT_ID, OWNER_ID, OTHER_ID));

                debtService.delete(DEBT_ID);
                verify(debtRS).delete(DEBT_ID);
            }
        }
    }

    // ─── getById ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("owner requests debt → returns full detail response with payments")
        void owner_returnsDebtWithPayments() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                final DebtDocument debt = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtRS.findById(DEBT_ID)).thenReturn(debt);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));
                when(debtMapper.convertToDetail(debt)).thenReturn(DebtDetailResponse.builder().id(DEBT_ID).build());

                final DebtDetailResponse response = debtService.getById(DEBT_ID);

                assertThat(response).isNotNull();
                assertThat(response.getId()).isEqualTo(DEBT_ID);
                verify(debtMapper).convertToDetail(debt);
            }
        }

        @Test
        @DisplayName("read-level member requests debt → succeeds")
        void readMember_succeeds() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OTHER_ID);

                final DebtDocument debt = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtRS.findById(DEBT_ID)).thenReturn(debt);
                when(categoryRS.findById(CAT_ID))
                        .thenReturn(buildCategoryWithReadMember(CAT_ID, OWNER_ID, OTHER_ID));
                when(debtMapper.convertToDetail(debt)).thenReturn(DebtDetailResponse.builder().id(DEBT_ID).build());

                assertThatNoException().isThrownBy(() -> debtService.getById(DEBT_ID));
            }
        }

        @Test
        @DisplayName("non-member requests debt → throws FORBIDDEN")
        void nonMember_throwsForbidden() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OTHER_ID);

                when(debtRS.findById(DEBT_ID)).thenReturn(buildDebt(DEBT_ID, OWNER_ID, CAT_ID));
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> debtService.getById(DEBT_ID))
                        .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);
            }
        }
    }

    // ─── get ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("get")
    class Get {

        @Test
        @DisplayName("isOwner=true returns owned categories with grouped debts")
        void get_returnsGroupedDebts() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                final CategoryDocument owned = buildCategory(CAT_ID, OWNER_ID);
                when(categoryRS.findByOwnerId(OWNER_ID)).thenReturn(List.of(owned));

                final DebtDocument debt = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtRS.findByCategoryIdIn(List.of(CAT_ID))).thenReturn(List.of(debt));

                final GetDebtResponse response = debtService.get(null, true);

                assertThat(response.getCategories()).hasSize(1);
                assertThat(response.getCategories().get(0).getCategoryId()).isEqualTo(CAT_ID);
            }
        }

        @Test
        @DisplayName("owned flag is set correctly based on isOwner param")
        void get_ownedFlagSetCorrectly() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                final CategoryDocument owned = buildCategory(CAT_ID, OWNER_ID);
                final CategoryDocument shared = buildCategory(NEW_CAT_ID, OTHER_ID);
                when(categoryRS.findByOwnerId(OWNER_ID)).thenReturn(List.of(owned));
                when(categoryRS.findByMemberUserId(OWNER_ID)).thenReturn(List.of(shared));
                when(debtRS.findByCategoryIdIn(anyList())).thenReturn(List.of());

                final GetDebtResponse ownedResponse = debtService.get(null, true);
                assertThat(ownedResponse.getCategories()).hasSize(1);
                assertThat(ownedResponse.getCategories().get(0).isOwned()).isTrue();
                assertThat(ownedResponse.getCategories().get(0).getOwnerUserId()).isNull();

                final GetDebtResponse sharedResponse = debtService.get(null, false);
                assertThat(sharedResponse.getCategories()).hasSize(1);
                assertThat(sharedResponse.getCategories().get(0).isOwned()).isFalse();
                assertThat(sharedResponse.getCategories().get(0).getOwnerUserId()).isEqualTo(OTHER_ID);
            }
        }

        @Test
        @DisplayName("no categories → returns empty list")
        void get_noCategories_returnsEmpty() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                when(categoryRS.findByOwnerId(OWNER_ID)).thenReturn(List.of());
                when(debtRS.findByCategoryIdIn(List.of())).thenReturn(List.of());

                final GetDebtResponse response = debtService.get(null, true);
                assertThat(response.getCategories()).isEmpty();
            }
        }
    }

    // ─── pay ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("pay")
    class Pay {

        @Test
        @DisplayName("owner pays debt → payment added to list, returns updated response")
        void ownerPays_addsPaymentAndReturns() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final ArgumentCaptor<DebtDocument> captor = ArgumentCaptor.forClass(DebtDocument.class);
                when(debtRS.update(captor.capture())).thenReturn(existing);
                when(debtMapper.convert(existing)).thenReturn(buildDebtItemResponse());

                final DebtResponse response = debtService.pay(DEBT_ID, null);

                assertThat(response.getDebt()).isNotNull();
                assertThat(captor.getValue().getPayments()).hasSize(1);
                assertThat(captor.getValue().getPayments().get(0).isPaid()).isTrue();
                assertThat(captor.getValue().getPayments().get(0).getPaidTime()).isNotNull();
            }
        }

        @Test
        @DisplayName("write-level member pays debt → succeeds")
        void writeMemberPays_succeeds() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OTHER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID))
                        .thenReturn(buildCategoryWithWriteMember(CAT_ID, OWNER_ID, OTHER_ID));
                when(debtRS.update(any())).thenReturn(existing);
                when(debtMapper.convert(existing)).thenReturn(buildDebtItemResponse());

                assertThatNoException().isThrownBy(() -> debtService.pay(DEBT_ID, null));
            }
        }

        @Test
        @DisplayName("payment from a previous period does not block new payment")
        void paymentFromPreviousPeriod_allowsNewPayment() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                // firstPaymentDate = now → current period starts now
                // existing payment was yesterday → belongs to previous period
                final List<PaymentEmbedded> existingPayments = new ArrayList<>();
                existingPayments.add(PaymentEmbedded.builder().paid(true).paidTime(LocalDateTime.now().minusDays(1)).build());
                final DebtDocument existing = DebtDocument.builder()
                        .id(DEBT_ID).ownerId(OWNER_ID).categoryId(CAT_ID)
                        .name("Test Debt").amount(BigDecimal.TEN)
                        .paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .firstPaymentDate(LocalDateTime.now())
                        .payments(existingPayments)
                        .build();
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final ArgumentCaptor<DebtDocument> captor = ArgumentCaptor.forClass(DebtDocument.class);
                when(debtRS.update(captor.capture())).thenReturn(existing);
                when(debtMapper.convert(existing)).thenReturn(buildDebtItemResponse());

                debtService.pay(DEBT_ID, null);

                assertThat(captor.getValue().getPayments()).hasSize(2);
            }
        }

        @Test
        @DisplayName("duplicate pay in same period → throws CONFLICT, no DB update")
        void duplicatePayInSamePeriod_throwsConflict() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                // firstPaymentDate = 5 days ago → current period started 5 days ago
                // existing payment = 2 days ago → within current period
                final LocalDateTime firstPaymentDate = LocalDateTime.now().minusDays(5);
                final List<PaymentEmbedded> existingPayments = new ArrayList<>();
                existingPayments.add(PaymentEmbedded.builder().paid(true)
                        .paidTime(LocalDateTime.now().minusDays(2)).build());
                final DebtDocument existing = DebtDocument.builder()
                        .id(DEBT_ID).ownerId(OWNER_ID).categoryId(CAT_ID)
                        .name("Test Debt").amount(BigDecimal.TEN)
                        .paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .firstPaymentDate(firstPaymentDate)
                        .payments(existingPayments)
                        .build();
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> debtService.pay(DEBT_ID, null))
                        .matches(e -> e.getStatus() == HttpStatus.CONFLICT);

                verify(debtRS, never()).update(any());
            }
        }

        @Test
        @DisplayName("ONE_TIME debt paid again → throws CONFLICT with error.debt.already_completed, no DB update")
        void oneTimeDebtPaidAgain_throwsAlreadyCompletedConflict() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                final List<PaymentEmbedded> existingPayments = new ArrayList<>();
                existingPayments.add(PaymentEmbedded.builder().paid(true)
                        .paidTime(LocalDateTime.now().minusDays(1)).build());
                // ONE_TIME debts are marked COMPLETED after their first payment (see below),
                // so a second pay() call must be rejected by the already_completed guard.
                final DebtDocument existing = DebtDocument.builder()
                        .id(DEBT_ID).ownerId(OWNER_ID).categoryId(CAT_ID)
                        .name("Test Debt").amount(BigDecimal.TEN)
                        .paymentPeriodType(PaymentPeriodType.ONE_TIME)
                        .firstPaymentDate(LocalDateTime.now().minusDays(2))
                        .payments(existingPayments)
                        .status(DebtStatus.COMPLETED)
                        .build();
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> debtService.pay(DEBT_ID, null))
                        .matches(e -> e.getStatus() == HttpStatus.CONFLICT)
                        .matches(e -> "error.debt.already_completed".equals(e.getCode()));

                verify(debtRS, never()).update(any());
                verify(categoryRS, never()).findById(any());
            }
        }

        @Test
        @DisplayName("ONE_TIME debt first payment → marked COMPLETED")
        void oneTimeDebtFirstPayment_marksCompleted() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                final DebtDocument existing = DebtDocument.builder()
                        .id(DEBT_ID).ownerId(OWNER_ID).categoryId(CAT_ID)
                        .name("Test Debt").amount(BigDecimal.TEN)
                        .paymentPeriodType(PaymentPeriodType.ONE_TIME)
                        .firstPaymentDate(LocalDateTime.now().minusDays(1))
                        .build();
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final ArgumentCaptor<DebtDocument> captor = ArgumentCaptor.forClass(DebtDocument.class);
                when(debtRS.update(captor.capture())).thenReturn(existing);
                when(debtMapper.convert(existing)).thenReturn(buildDebtItemResponse());

                debtService.pay(DEBT_ID, null);

                assertThat(captor.getValue().getStatus()).isEqualTo(DebtStatus.COMPLETED);
            }
        }

        @Test
        @DisplayName("pay after full period has elapsed → allowed, not treated as duplicate")
        void payAfterNewPeriodStarted_allowsPayment() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                // firstPaymentDate = 35 days ago → current MONTHLY period started 5 days ago
                // existing payment = 30 days ago → in the previous period
                final LocalDateTime firstPaymentDate = LocalDateTime.now().minusDays(35);
                final List<PaymentEmbedded> existingPayments = new ArrayList<>();
                existingPayments.add(PaymentEmbedded.builder().paid(true)
                        .paidTime(LocalDateTime.now().minusDays(30)).build());
                final DebtDocument existing = DebtDocument.builder()
                        .id(DEBT_ID).ownerId(OWNER_ID).categoryId(CAT_ID)
                        .name("Test Debt").amount(BigDecimal.TEN)
                        .paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .firstPaymentDate(firstPaymentDate)
                        .payments(existingPayments)
                        .build();
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final ArgumentCaptor<DebtDocument> captor = ArgumentCaptor.forClass(DebtDocument.class);
                when(debtRS.update(captor.capture())).thenReturn(existing);
                when(debtMapper.convert(existing)).thenReturn(buildDebtItemResponse());

                debtService.pay(DEBT_ID, null);

                assertThat(captor.getValue().getPayments()).hasSize(2);
            }
        }

        @Test
        @DisplayName("no write access → throws FORBIDDEN, debt not updated")
        void noWriteAccess_throwsForbidden() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OTHER_ID);

                when(debtRS.findById(DEBT_ID)).thenReturn(buildDebt(DEBT_ID, OWNER_ID, CAT_ID));
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> debtService.pay(DEBT_ID, null))
                        .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);

                verify(debtRS, never()).update(any());
            }
        }

        @Test
        @DisplayName("auto-payment enabled → throws CONFLICT, no DB call")
        void autoPaymentEnabled_throwsConflict() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                final DebtDocument autoDebt = DebtDocument.builder()
                        .id(DEBT_ID).ownerId(OWNER_ID).categoryId(CAT_ID)
                        .name("Test").amount(BigDecimal.TEN)
                        .paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .firstPaymentDate(LocalDateTime.now())
                        .autoPayment(true)
                        .build();
                when(debtRS.findById(DEBT_ID)).thenReturn(autoDebt);

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> debtService.pay(DEBT_ID, null))
                        .matches(e -> e.getStatus() == HttpStatus.CONFLICT);

                verify(debtRS, never()).update(any());
                verify(categoryRS, never()).findById(any());
            }
        }

        @Test
        @DisplayName("installmentCount reached → lifecycle status set to COMPLETED and payment status PAID")
        void installmentCountReached_statusCompletedAndPaid() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                final DebtDocument existing = DebtDocument.builder()
                        .id(DEBT_ID).ownerId(OWNER_ID).categoryId(CAT_ID)
                        .name("Test Debt").amount(BigDecimal.TEN)
                        .paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .firstPaymentDate(LocalDateTime.now().minusMonths(1))
                        .installmentCount(1)
                        .build();
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final ArgumentCaptor<DebtDocument> captor = ArgumentCaptor.forClass(DebtDocument.class);
                when(debtRS.update(captor.capture())).thenReturn(existing);
                when(debtMapper.convert(existing)).thenReturn(buildDebtItemResponse());

                debtService.pay(DEBT_ID, null);

                assertThat(captor.getValue().getStatus()).isEqualTo(DebtStatus.COMPLETED);
                assertThat(captor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
            }
        }

        @Test
        @DisplayName("installmentCount not yet reached → lifecycle status remains ACTIVE and payment status PAID")
        void installmentCountNotReached_statusActiveAndPaid() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);
                ctx.when(RequestContextUtil::isPremium).thenReturn(true);
                ctx.when(RequestContextUtil::getLocale).thenReturn("en");

                final DebtDocument existing = DebtDocument.builder()
                        .id(DEBT_ID).ownerId(OWNER_ID).categoryId(CAT_ID)
                        .name("Test Debt").amount(BigDecimal.TEN)
                        .paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .firstPaymentDate(LocalDateTime.now())
                        .installmentCount(6)
                        .build();
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final ArgumentCaptor<DebtDocument> captor = ArgumentCaptor.forClass(DebtDocument.class);
                when(debtRS.update(captor.capture())).thenReturn(existing);
                when(debtMapper.convert(existing)).thenReturn(buildDebtItemResponse());

                debtService.pay(DEBT_ID, null);

                assertThat(captor.getValue().getStatus()).isEqualTo(DebtStatus.ACTIVE);
                assertThat(captor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
            }
        }

        @Test
        @DisplayName("partial payment → amountPaid stored as provided value")
        void partialPayment_usesProvidedAmount() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final ArgumentCaptor<DebtDocument> captor = ArgumentCaptor.forClass(DebtDocument.class);
                when(debtRS.update(captor.capture())).thenReturn(existing);
                when(debtMapper.convert(existing)).thenReturn(buildDebtItemResponse());

                final BigDecimal partial = new BigDecimal("5");
                debtService.pay(DEBT_ID, PayDebtRequest.builder().amountPaid(partial).build());

                assertThat(captor.getValue().getPayments()).hasSize(1);
                assertThat(captor.getValue().getPayments().get(0).getAmountPaid()).isEqualByComparingTo(partial);
            }
        }

        @Test
        @DisplayName("null request → amountPaid defaults to debt amount")
        void nullRequest_usesFullAmount() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final ArgumentCaptor<DebtDocument> captor = ArgumentCaptor.forClass(DebtDocument.class);
                when(debtRS.update(captor.capture())).thenReturn(existing);
                when(debtMapper.convert(existing)).thenReturn(buildDebtItemResponse());

                debtService.pay(DEBT_ID, null);

                assertThat(captor.getValue().getPayments()).hasSize(1);
                assertThat(captor.getValue().getPayments().get(0).getAmountPaid())
                        .isEqualByComparingTo(existing.getAmount());
            }
        }

        @Test
        @DisplayName("amountPaid exceeds amount due → throws BAD_REQUEST error.debt.overpayment")
        void amountExceedsDue_throwsOverpayment() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final BigDecimal tooMuch = existing.getAmount().add(BigDecimal.ONE);

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> debtService.pay(DEBT_ID, PayDebtRequest.builder().amountPaid(tooMuch).build()))
                        .matches(e -> e.getStatus() == HttpStatus.BAD_REQUEST)
                        .matches(e -> "error.debt.overpayment".equals(e.getCode()));

                verify(debtRS, never()).update(any());
            }
        }
    }

    // ─── skip ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("skip")
    class Skip {

        @Test
        @DisplayName("owner skips → adds skipped payment and returns response")
        void ownerSkips_addsSkippedPaymentAndReturns() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final ArgumentCaptor<DebtDocument> captor = ArgumentCaptor.forClass(DebtDocument.class);
                when(debtRS.update(captor.capture())).thenReturn(existing);
                when(debtMapper.convert(existing)).thenReturn(buildDebtItemResponse());

                final DebtResponse response = debtService.skip(DEBT_ID, null);

                assertThat(response.getDebt()).isNotNull();
                assertThat(captor.getValue().getPayments()).hasSize(1);
                assertThat(captor.getValue().getPayments().get(0).isPaid()).isFalse();
            }
        }

        @Test
        @DisplayName("already completed → throws conflict")
        void alreadyCompleted_throwsConflict() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                existing.setStatus(DebtStatus.COMPLETED);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);

                final Throwable t = catchThrowable(() -> debtService.skip(DEBT_ID, null));

                assertThat(t).isInstanceOf(BaseException.class);
                assertThat(((BaseException) t).getStatus()).isEqualTo(HttpStatus.CONFLICT);
            }
        }

        @Test
        @DisplayName("auto payment enabled → throws conflict")
        void autoPaymentEnabled_throwsConflict() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                existing.setAutoPayment(true);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);

                final Throwable t = catchThrowable(() -> debtService.skip(DEBT_ID, null));

                assertThat(t).isInstanceOf(BaseException.class);
                assertThat(((BaseException) t).getStatus()).isEqualTo(HttpStatus.CONFLICT);
            }
        }

        @Test
        @DisplayName("no write access → throws forbidden")
        void noWriteAccess_throwsForbidden() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OTHER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final Throwable t = catchThrowable(() -> debtService.skip(DEBT_ID, null));

                assertThat(t).isInstanceOf(BaseException.class);
                assertThat(((BaseException) t).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            }
        }

        @Test
        @DisplayName("period already recorded → throws conflict")
        void periodAlreadyRecorded_throwsConflict() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                final LocalDateTime now = LocalDateTime.now();
                existing.addPayment(now, now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0),
                        now.withDayOfMonth(1).plusMonths(1).minusDays(1), BigDecimal.TEN);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final Throwable t = catchThrowable(() -> debtService.skip(DEBT_ID, null));

                assertThat(t).isInstanceOf(BaseException.class);
                assertThat(((BaseException) t).getStatus()).isEqualTo(HttpStatus.CONFLICT);
            }
        }
    }

    // ─── bulkPay ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("bulkPay")
    class BulkPay {

        @Test
        @DisplayName("owner bulk pays two distinct periods → both payments recorded")
        void ownerBulkPays_twoPaymentsRecorded() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                existing.setFirstPaymentDate(LocalDateTime.now().minusMonths(3));
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final ArgumentCaptor<DebtDocument> captor = ArgumentCaptor.forClass(DebtDocument.class);
                when(debtRS.update(captor.capture())).thenReturn(existing);
                when(debtMapper.convert(existing)).thenReturn(buildDebtItemResponse());

                final List<PayDebtRequest> requests = List.of(
                        PayDebtRequest.builder().paidTime(LocalDateTime.now().minusMonths(2)).build(),
                        PayDebtRequest.builder().paidTime(LocalDateTime.now().minusMonths(1)).build()
                );

                final DebtResponse response = debtService.bulkPay(DEBT_ID, requests);

                assertThat(response.getDebt()).isNotNull();
                assertThat(captor.getValue().getPayments()).hasSize(2);
            }
        }

        @Test
        @DisplayName("amountPaid exceeds amount due → throws BAD_REQUEST error.debt.overpayment")
        void amountExceedsDue_throwsOverpayment() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final BigDecimal tooMuch = existing.getAmount().add(BigDecimal.ONE);
                final List<PayDebtRequest> requests = List.of(
                        PayDebtRequest.builder().amountPaid(tooMuch).build());

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> debtService.bulkPay(DEBT_ID, requests))
                        .matches(e -> e.getStatus() == HttpStatus.BAD_REQUEST)
                        .matches(e -> "error.debt.overpayment".equals(e.getCode()));

                verify(debtRS, never()).update(any());
            }
        }

        @Test
        @DisplayName("already completed → throws conflict")
        void alreadyCompleted_throwsConflict() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                existing.setStatus(DebtStatus.COMPLETED);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);

                final Throwable t = catchThrowable(() -> debtService.bulkPay(DEBT_ID, List.of()));

                assertThat(t).isInstanceOf(BaseException.class);
                assertThat(((BaseException) t).getStatus()).isEqualTo(HttpStatus.CONFLICT);
            }
        }

        @Test
        @DisplayName("auto payment enabled → throws conflict")
        void autoPaymentEnabled_throwsConflict() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                existing.setAutoPayment(true);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);

                final Throwable t = catchThrowable(() -> debtService.bulkPay(DEBT_ID, List.of()));

                assertThat(t).isInstanceOf(BaseException.class);
                assertThat(((BaseException) t).getStatus()).isEqualTo(HttpStatus.CONFLICT);
            }
        }

        @Test
        @DisplayName("no write access → throws forbidden")
        void noWriteAccess_throwsForbidden() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OTHER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final Throwable t = catchThrowable(() -> debtService.bulkPay(DEBT_ID, List.of()));

                assertThat(t).isInstanceOf(BaseException.class);
                assertThat(((BaseException) t).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            }
        }

        @Test
        @DisplayName("installment count reached → status set to COMPLETED")
        void installmentCountReached_statusCompleted() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                // Use fixed base date so period boundaries are predictable
                final LocalDateTime base = LocalDateTime.now().minusMonths(3).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                existing.setFirstPaymentDate(base);
                existing.setInstallmentCount(2);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final ArgumentCaptor<DebtDocument> captor = ArgumentCaptor.forClass(DebtDocument.class);
                when(debtRS.update(captor.capture())).thenReturn(existing);
                when(debtMapper.convert(existing)).thenReturn(buildDebtItemResponse());

                // Two payments exactly one month apart, both in distinct periods relative to base
                final List<PayDebtRequest> requests = List.of(
                        PayDebtRequest.builder().paidTime(base.plusDays(5)).build(),
                        PayDebtRequest.builder().paidTime(base.plusMonths(1).plusDays(5)).build()
                );

                debtService.bulkPay(DEBT_ID, requests);

                assertThat(captor.getValue().getStatus()).isEqualTo(DebtStatus.COMPLETED);
            }
        }

        @Test
        @DisplayName("duplicate period in bulk → skipped silently")
        void duplicatePeriod_skippedSilently() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final LocalDateTime base = LocalDateTime.now().minusMonths(3).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                existing.setFirstPaymentDate(base);
                // Pre-populate a payment in base period
                existing.addPayment(base.plusDays(5), base, base.plusMonths(1).minusDays(1), BigDecimal.TEN);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final ArgumentCaptor<DebtDocument> captor = ArgumentCaptor.forClass(DebtDocument.class);
                when(debtRS.update(captor.capture())).thenReturn(existing);
                when(debtMapper.convert(existing)).thenReturn(buildDebtItemResponse());

                // Send a request for the same period — should be silently skipped
                debtService.bulkPay(DEBT_ID, List.of(PayDebtRequest.builder().paidTime(base.plusDays(10)).build()));

                assertThat(captor.getValue().getPayments()).hasSize(1);
            }
        }
    }

    // ─── setPaidInstallments ──────────────────────────────────────────────────

    @Nested
    @DisplayName("setPaidInstallments")
    class SetPaidInstallments {

        @Test
        @DisplayName("owner sets 2 installments → 2 payments added")
        void ownerSetsInstallments_paymentsAdded() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                existing.setFirstPaymentDate(LocalDateTime.now().minusMonths(3));
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final ArgumentCaptor<DebtDocument> captor = ArgumentCaptor.forClass(DebtDocument.class);
                when(debtRS.update(captor.capture())).thenReturn(existing);
                when(debtMapper.convert(existing)).thenReturn(buildDebtItemResponse());

                debtService.setPaidInstallments(DEBT_ID, SetPaidInstallmentsRequest.builder().count(2).build());

                assertThat(captor.getValue().getPayments()).hasSize(2);
                assertThat(captor.getValue().getPayments()).allMatch(PaymentEmbedded::isPaid);
            }
        }

        @Test
        @DisplayName("already completed → throws conflict")
        void alreadyCompleted_throwsConflict() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                existing.setStatus(DebtStatus.COMPLETED);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);

                final Throwable t = catchThrowable(() ->
                        debtService.setPaidInstallments(DEBT_ID, SetPaidInstallmentsRequest.builder().count(1).build()));

                assertThat(t).isInstanceOf(BaseException.class);
                assertThat(((BaseException) t).getStatus()).isEqualTo(HttpStatus.CONFLICT);
            }
        }

        @Test
        @DisplayName("auto payment enabled → throws conflict")
        void autoPaymentEnabled_throwsConflict() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                existing.setAutoPayment(true);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);

                final Throwable t = catchThrowable(() ->
                        debtService.setPaidInstallments(DEBT_ID, SetPaidInstallmentsRequest.builder().count(1).build()));

                assertThat(t).isInstanceOf(BaseException.class);
                assertThat(((BaseException) t).getStatus()).isEqualTo(HttpStatus.CONFLICT);
            }
        }

        @Test
        @DisplayName("missing period config → throws bad request")
        void missingPeriodConfig_throwsBadRequest() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final DebtDocument existing = DebtDocument.builder()
                        .id(DEBT_ID)
                        .ownerId(OWNER_ID)
                        .categoryId(CAT_ID)
                        .amount(BigDecimal.TEN)
                        .status(DebtStatus.ACTIVE)
                        .payments(new ArrayList<>())
                        .build();
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);

                final Throwable t = catchThrowable(() ->
                        debtService.setPaidInstallments(DEBT_ID, SetPaidInstallmentsRequest.builder().count(1).build()));

                assertThat(t).isInstanceOf(BaseException.class);
                assertThat(((BaseException) t).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            }
        }

        @Test
        @DisplayName("count exceeds installment count → throws bad request")
        void countExceedsInstallmentCount_throwsBadRequest() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                existing.setInstallmentCount(3);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);

                final Throwable t = catchThrowable(() ->
                        debtService.setPaidInstallments(DEBT_ID, SetPaidInstallmentsRequest.builder().count(5).build()));

                assertThat(t).isInstanceOf(BaseException.class);
                assertThat(((BaseException) t).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            }
        }

        @Test
        @DisplayName("no write access → throws forbidden")
        void noWriteAccess_throwsForbidden() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OTHER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final Throwable t = catchThrowable(() ->
                        debtService.setPaidInstallments(DEBT_ID, SetPaidInstallmentsRequest.builder().count(1).build()));

                assertThat(t).isInstanceOf(BaseException.class);
                assertThat(((BaseException) t).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            }
        }

        @Test
        @DisplayName("installment count reached → status set to COMPLETED")
        void installmentCountReached_statusCompleted() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                existing.setFirstPaymentDate(LocalDateTime.now().minusMonths(3));
                existing.setInstallmentCount(2);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final ArgumentCaptor<DebtDocument> captor = ArgumentCaptor.forClass(DebtDocument.class);
                when(debtRS.update(captor.capture())).thenReturn(existing);
                when(debtMapper.convert(existing)).thenReturn(buildDebtItemResponse());

                debtService.setPaidInstallments(DEBT_ID, SetPaidInstallmentsRequest.builder().count(2).build());

                assertThat(captor.getValue().getStatus()).isEqualTo(DebtStatus.COMPLETED);
            }
        }
    }

    // ─── getPaymentHistory ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPaymentHistory")
    class GetPaymentHistory {

        @Test
        @DisplayName("owner requests history → returns payments within range")
        void owner_returnsPaymentsWithinRange() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final LocalDateTime now = LocalDateTime.now();
                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                existing.addPayment(now.minusDays(5), now.minusDays(10), now.minusDays(1), BigDecimal.TEN);
                existing.addPayment(now.minusDays(40), now.minusDays(45), now.minusDays(35), BigDecimal.TEN);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final List<PaymentHistoryResponse> history = debtService.getPaymentHistory(
                        DEBT_ID, now.minusDays(20), now);

                assertThat(history).hasSize(1);
                assertThat(history.get(0).isPaid()).isTrue();
            }
        }

        @Test
        @DisplayName("no read access → throws forbidden")
        void noReadAccess_throwsForbidden() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OTHER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategory(CAT_ID, OWNER_ID));

                final LocalDateTime now = LocalDateTime.now();
                final Throwable t = catchThrowable(() ->
                        debtService.getPaymentHistory(DEBT_ID, now.minusMonths(1), now));

                assertThat(t).isInstanceOf(BaseException.class);
                assertThat(((BaseException) t).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            }
        }

        @Test
        @DisplayName("read member → can access payment history")
        void readMember_canAccessHistory() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OTHER_ID);

                final DebtDocument existing = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                when(debtRS.findById(DEBT_ID)).thenReturn(existing);
                when(categoryRS.findById(CAT_ID)).thenReturn(buildCategoryWithReadMember(CAT_ID, OWNER_ID, OTHER_ID));

                final LocalDateTime now = LocalDateTime.now();
                final List<PaymentHistoryResponse> history =
                        debtService.getPaymentHistory(DEBT_ID, now.minusMonths(1), now);

                assertThat(history).isEmpty();
            }
        }
    }

    // ─── getMonthlySummary ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMonthlySummary")
    class GetMonthlySummary {

        @Test
        @DisplayName("no debts → returns zeroed summary")
        void noDebts_returnsZeroedSummary() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                when(categoryRS.findByOwnerId(OWNER_ID)).thenReturn(List.of());
                when(categoryRS.findByMemberUserId(OWNER_ID)).thenReturn(List.of());
                when(debtRS.findByCategoryIdIn(List.of())).thenReturn(List.of());
                when(incomeRS.findByOwnerIdAsOf(eq(OWNER_ID), any())).thenReturn(Optional.empty());

                final String month = YearMonth.now().toString();
                final MonthlySummaryResponse summary = debtService.getMonthlySummary(month);

                assertThat(summary.getTotalPaid()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(summary.getPaidCount()).isZero();
                assertThat(summary.getSkippedCount()).isZero();
                assertThat(summary.getPendingCount()).isZero();
                assertThat(summary.getDebts()).isEmpty();
            }
        }

        @Test
        @DisplayName("paid debt in month → paidCount=1 and totalPaid equals debt amount")
        void paidDebtInMonth_paidCountAndTotalPaidCorrect() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final YearMonth ym = YearMonth.now().minusMonths(1);
                final LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
                final LocalDateTime monthEnd = ym.atEndOfMonth().atTime(23, 59, 59);

                final CategoryDocument cat = buildCategory(CAT_ID, OWNER_ID);
                final DebtDocument debt = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                debt.setFirstPaymentDate(monthStart);
                final LocalDateTime payTime = monthStart.plusDays(5);
                debt.addPayment(payTime, monthStart, monthEnd, BigDecimal.valueOf(100));

                when(categoryRS.findByOwnerId(OWNER_ID)).thenReturn(List.of(cat));
                when(categoryRS.findByMemberUserId(OWNER_ID)).thenReturn(List.of());
                when(debtRS.findByCategoryIdIn(List.of(CAT_ID))).thenReturn(List.of(debt));
                when(incomeRS.findByOwnerIdAsOf(eq(OWNER_ID), any())).thenReturn(Optional.empty());

                final MonthlySummaryResponse summary = debtService.getMonthlySummary(ym.toString());

                assertThat(summary.getPaidCount()).isEqualTo(1);
                assertThat(summary.getTotalPaid()).isEqualByComparingTo(BigDecimal.valueOf(100));
                assertThat(summary.getSkippedCount()).isZero();
            }
        }

        @Test
        @DisplayName("skipped debt in month → skippedCount=1")
        void skippedDebtInMonth_skippedCountCorrect() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final YearMonth ym = YearMonth.now().minusMonths(1);
                final LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
                final LocalDateTime monthEnd = ym.atEndOfMonth().atTime(23, 59, 59);

                final CategoryDocument cat = buildCategory(CAT_ID, OWNER_ID);
                final DebtDocument debt = buildDebt(DEBT_ID, OWNER_ID, CAT_ID);
                debt.setFirstPaymentDate(monthStart);
                debt.skipPayment(monthStart.plusDays(3), monthStart, monthEnd);

                when(categoryRS.findByOwnerId(OWNER_ID)).thenReturn(List.of(cat));
                when(categoryRS.findByMemberUserId(OWNER_ID)).thenReturn(List.of());
                when(debtRS.findByCategoryIdIn(List.of(CAT_ID))).thenReturn(List.of(debt));
                when(incomeRS.findByOwnerIdAsOf(eq(OWNER_ID), any())).thenReturn(Optional.empty());

                final MonthlySummaryResponse summary = debtService.getMonthlySummary(ym.toString());

                assertThat(summary.getSkippedCount()).isEqualTo(1);
                assertThat(summary.getPaidCount()).isZero();
            }
        }

        @Test
        @DisplayName("pending debt (no payment, active, started before month end) → pendingCount=1")
        void pendingDebt_pendingCountCorrect() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final YearMonth ym = YearMonth.now().minusMonths(1);
                final LocalDateTime monthStart = ym.atDay(1).atStartOfDay();

                final CategoryDocument cat = buildCategory(CAT_ID, OWNER_ID);
                final DebtDocument debt = DebtDocument.builder()
                        .id(DEBT_ID)
                        .ownerId(OWNER_ID)
                        .categoryId(CAT_ID)
                        .amount(BigDecimal.valueOf(50))
                        .status(DebtStatus.ACTIVE)
                        .payments(new ArrayList<>())
                        .firstPaymentDate(monthStart)
                        .paymentPeriodType(PaymentPeriodType.MONTHLY)
                        .build();

                when(categoryRS.findByOwnerId(OWNER_ID)).thenReturn(List.of(cat));
                when(categoryRS.findByMemberUserId(OWNER_ID)).thenReturn(List.of());
                when(debtRS.findByCategoryIdIn(List.of(CAT_ID))).thenReturn(List.of(debt));
                when(incomeRS.findByOwnerIdAsOf(eq(OWNER_ID), any())).thenReturn(Optional.empty());

                final MonthlySummaryResponse summary = debtService.getMonthlySummary(ym.toString());

                assertThat(summary.getPendingCount()).isEqualTo(1);
                assertThat(summary.getPendingAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));
            }
        }
    }
}

