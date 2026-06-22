package dev.batuhanyetgin.kamci.service;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import dev.batuhanyetgin.kamci.document.UserIncomeDocument;
import dev.batuhanyetgin.kamci.dto.request.CreateIncomeRequest;
import dev.batuhanyetgin.kamci.dto.response.IncomeResponse;
import dev.batuhanyetgin.kamci.enums.CurrencyType;
import dev.batuhanyetgin.kamci.repositoryservice.UserIncomeRepositoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncomeServiceTest {

    @Mock private UserIncomeRepositoryService incomeRS;

    @InjectMocks
    private IncomeService incomeService;

    private static final String USER_ID    = "user123";
    private static final String INCOME_ID  = "income456";

    private UserIncomeDocument buildDoc() {
        return UserIncomeDocument.builder()
                .ownerId(USER_ID)
                .amount(BigDecimal.valueOf(5000))
                .currency(CurrencyType.TRY)
                .effectiveFrom(LocalDateTime.of(2025, 1, 1, 0, 0))
                .note("Salary")
                .build();
    }

    private CreateIncomeRequest buildRequest() {
        return CreateIncomeRequest.builder()
                .amount(BigDecimal.valueOf(5000))
                .currency(CurrencyType.TRY)
                .effectiveFrom(LocalDateTime.of(2025, 1, 1, 0, 0))
                .note("Salary")
                .build();
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("valid request → saves document and returns response")
        void validRequest_savesAndReturns() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(USER_ID);

                final UserIncomeDocument saved = buildDoc();
                org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", INCOME_ID);
                when(incomeRS.save(any(UserIncomeDocument.class))).thenReturn(saved);

                final IncomeResponse response = incomeService.create(buildRequest());

                assertThat(response.getId()).isEqualTo(INCOME_ID);
                assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000));
                assertThat(response.getCurrency()).isEqualTo(CurrencyType.TRY);
                assertThat(response.getNote()).isEqualTo("Salary");
                verify(incomeRS).save(any(UserIncomeDocument.class));
            }
        }
    }

    // ─── getHistory ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getHistory")
    class GetHistory {

        @Test
        @DisplayName("user has two records → returns both mapped")
        void hasTwoRecords_returnsBothMapped() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(USER_ID);

                final UserIncomeDocument doc1 = buildDoc();
                final UserIncomeDocument doc2 = buildDoc();
                when(incomeRS.findByOwnerIdOrderByEffectiveFromDesc(USER_ID))
                        .thenReturn(List.of(doc1, doc2));

                final List<IncomeResponse> history = incomeService.getHistory();

                assertThat(history).hasSize(2);
            }
        }

        @Test
        @DisplayName("no records → returns empty list")
        void noRecords_returnsEmpty() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(USER_ID);

                when(incomeRS.findByOwnerIdOrderByEffectiveFromDesc(USER_ID)).thenReturn(List.of());

                assertThat(incomeService.getHistory()).isEmpty();
            }
        }
    }

    // ─── getCurrent ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCurrent")
    class GetCurrent {

        @Test
        @DisplayName("current income exists → returns response")
        void currentExists_returnsResponse() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(USER_ID);

                when(incomeRS.findCurrentByOwnerId(USER_ID)).thenReturn(Optional.of(buildDoc()));

                final IncomeResponse response = incomeService.getCurrent();

                assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000));
            }
        }

        @Test
        @DisplayName("no income record → throws NOT_FOUND")
        void noRecord_throwsNotFound() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(USER_ID);

                when(incomeRS.findCurrentByOwnerId(USER_ID)).thenReturn(Optional.empty());

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> incomeService.getCurrent())
                        .matches(e -> e.getStatus() == HttpStatus.NOT_FOUND);
            }
        }
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("owner deletes own record → calls incomeRS.delete")
        void ownerDeletes_deletesRecord() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(USER_ID);

                final UserIncomeDocument doc = buildDoc();
                org.springframework.test.util.ReflectionTestUtils.setField(doc, "id", INCOME_ID);
                when(incomeRS.findById(INCOME_ID)).thenReturn(doc);

                assertThatNoException().isThrownBy(() -> incomeService.delete(INCOME_ID));
                verify(incomeRS).delete(INCOME_ID);
            }
        }

        @Test
        @DisplayName("non-owner deletes → throws FORBIDDEN")
        void nonOwnerDeletes_throwsForbidden() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn("other999");

                final UserIncomeDocument doc = buildDoc();
                when(incomeRS.findById(INCOME_ID)).thenReturn(doc);

                assertThatExceptionOfType(BaseException.class)
                        .isThrownBy(() -> incomeService.delete(INCOME_ID))
                        .matches(e -> e.getStatus() == HttpStatus.FORBIDDEN);

                verify(incomeRS, never()).delete(any());
            }
        }
    }
}
