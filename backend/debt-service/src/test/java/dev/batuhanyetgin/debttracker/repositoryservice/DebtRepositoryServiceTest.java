package dev.batuhanyetgin.debttracker.repositoryservice;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.enums.DebtStatus;
import dev.batuhanyetgin.debttracker.enums.PaymentPeriodType;
import dev.batuhanyetgin.debttracker.repository.DebtRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebtRepositoryServiceTest {

    @Mock private DebtRepository debtRepository;

    private DebtRepositoryService debtRepositoryService;

    private static final String DEBT_ID   = "debt123";
    private static final String CAT_ID    = "cat123";
    private static final String OWNER_ID  = "owner123";

    @BeforeEach
    void setUp() {
        debtRepositoryService = new DebtRepositoryService(debtRepository);
    }

    private DebtDocument buildDebt() {
        return DebtDocument.builder()
                .id(DEBT_ID)
                .name("Test Debt")
                .ownerId(OWNER_ID)
                .categoryId(CAT_ID)
                .amount(BigDecimal.TEN)
                .paymentPeriodType(PaymentPeriodType.MONTHLY)
                .firstPaymentDate(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("existing id → returns debt")
        void existingId_returnsDebt() {
            final DebtDocument debt = buildDebt();
            when(debtRepository.findById(DEBT_ID)).thenReturn(Optional.of(debt));
            assertThat(debtRepositoryService.findById(DEBT_ID)).isEqualTo(debt);
        }

        @Test
        @DisplayName("missing id → throws NOT_FOUND")
        void missingId_throwsNotFound() {
            when(debtRepository.findById(DEBT_ID)).thenReturn(Optional.empty());
            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> debtRepositoryService.findById(DEBT_ID))
                    .matches(e -> e.getStatus() == HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("save / update")
    class SaveUpdate {

        @Test
        @DisplayName("save delegates to repository")
        void save_delegatesToRepo() {
            final DebtDocument debt = buildDebt();
            when(debtRepository.save(debt)).thenReturn(debt);
            assertThat(debtRepositoryService.save(debt)).isEqualTo(debt);
            verify(debtRepository).save(debt);
        }

        @Test
        @DisplayName("update delegates to repository save")
        void update_delegatesToRepo() {
            final DebtDocument debt = buildDebt();
            when(debtRepository.save(debt)).thenReturn(debt);
            assertThat(debtRepositoryService.update(debt)).isEqualTo(debt);
            verify(debtRepository).save(debt);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("calls deleteById on repository")
        void delete_callsDeleteById() {
            debtRepositoryService.delete(DEBT_ID);
            verify(debtRepository).deleteById(DEBT_ID);
        }

        @Test
        @DisplayName("deleteByCategoryId delegates to repository")
        void deleteByCategoryId_delegates() {
            debtRepositoryService.deleteByCategoryId(CAT_ID);
            verify(debtRepository).deleteByCategoryId(CAT_ID);
        }
    }

    @Nested
    @DisplayName("query methods")
    class QueryMethods {

        @Test
        @DisplayName("findByCategoryId returns list")
        void findByCategoryId_returnsList() {
            final List<DebtDocument> debts = List.of(buildDebt());
            when(debtRepository.findByCategoryId(CAT_ID)).thenReturn(debts);
            assertThat(debtRepositoryService.findByCategoryId(CAT_ID)).isEqualTo(debts);
        }

        @Test
        @DisplayName("findByCategoryIdIn returns list")
        void findByCategoryIdIn_returnsList() {
            final List<DebtDocument> debts = List.of(buildDebt());
            when(debtRepository.findByCategoryIdIn(List.of(CAT_ID))).thenReturn(debts);
            assertThat(debtRepositoryService.findByCategoryIdIn(List.of(CAT_ID))).isEqualTo(debts);
        }

        @Test
        @DisplayName("existsByCategoryId returns true when exists")
        void existsByCategoryId_returnsTrue() {
            when(debtRepository.existsByCategoryId(CAT_ID)).thenReturn(true);
            assertThat(debtRepositoryService.existsByCategoryId(CAT_ID)).isTrue();
        }

        @Test
        @DisplayName("countByOwnerId returns count")
        void countByOwnerId_returnsCount() {
            when(debtRepository.countByOwnerId(OWNER_ID)).thenReturn(5L);
            assertThat(debtRepositoryService.countByOwnerId(OWNER_ID)).isEqualTo(5L);
        }

        @Test
        @DisplayName("findDueReminders returns unpaid debts with past reminder date")
        void findDueReminders_returnsDebts() {
            final LocalDateTime now = LocalDateTime.now();
            final List<DebtDocument> debts = List.of(buildDebt());
            when(debtRepository.findByStatusAndReminderEnabledTrueAndNextReminderDateLessThanEqual(
                    DebtStatus.UNPAID, now)).thenReturn(debts);
            assertThat(debtRepositoryService.findDueReminders(now)).isEqualTo(debts);
        }

        @Test
        @DisplayName("findDueAutoPayments returns auto-payment debts with past next-auto-payment date")
        void findDueAutoPayments_returnsDebts() {
            final LocalDateTime now = LocalDateTime.now();
            final List<DebtDocument> debts = List.of(buildDebt());
            when(debtRepository.findByAutoPaymentTrueAndNextAutoPaymentDateLessThanEqual(now))
                    .thenReturn(debts);
            assertThat(debtRepositoryService.findDueAutoPayments(now)).isEqualTo(debts);
        }
    }
}
