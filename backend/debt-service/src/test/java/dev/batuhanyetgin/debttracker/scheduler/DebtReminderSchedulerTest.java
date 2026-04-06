package dev.batuhanyetgin.debttracker.scheduler;

import dev.batuhanyetgin.commonmodule.event.ReminderChannelType;
import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.document.embedded.ReminderEmbedded;
import dev.batuhanyetgin.debttracker.enums.DebtStatus;
import dev.batuhanyetgin.debttracker.enums.PaymentStatus;
import dev.batuhanyetgin.debttracker.enums.PaymentPeriodType;
import dev.batuhanyetgin.debttracker.producer.DebtEventProducer;
import dev.batuhanyetgin.debttracker.repositoryservice.DebtRepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebtReminderSchedulerTest {

    @Mock private DebtRepositoryService debtRS;
    @Mock private DebtEventProducer debtEventProducer;

    private final Executor taskExecutor = Runnable::run;

    private DebtReminderScheduler scheduler;

    // April 1 2026 — day-of-month = 1, safe for all MONTHLY clamp tests
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 4, 1, 10, 0);
    // Jan 31 — used for day-drift tests
    private static final LocalDateTime JAN_31 = LocalDateTime.of(2026, 1, 31, 10, 0);

    @BeforeEach
    void setUp() {
        scheduler = new DebtReminderScheduler(debtRS, debtEventProducer, taskExecutor);
    }

    private DebtDocument buildReminderDebt(final String id, final PaymentPeriodType periodType,
                                           final LocalDateTime firstPaymentDate,
                                           final LocalDateTime nextReminder) {
        return DebtDocument.builder()
                .id(id)
                .ownerId("owner1")
                .name("Test Debt")
                .amount(BigDecimal.TEN)
                .paymentPeriodType(periodType)
                .firstPaymentDate(firstPaymentDate)
                .nextReminderDate(nextReminder)
                .reminder(ReminderEmbedded.builder()
                        .enabled(true)
                        .channels(List.of(ReminderChannelType.IN_APP))
                        .build())
                .build();
    }

    private DebtDocument buildAutoPaymentDebt(final String id, final PaymentPeriodType periodType,
                                              final LocalDateTime firstPaymentDate,
                                              final LocalDateTime nextAutoPayment) {
        return DebtDocument.builder()
                .id(id)
                .ownerId("owner1")
                .name("Auto Debt")
                .amount(BigDecimal.TEN)
                .paymentPeriodType(periodType)
                .firstPaymentDate(firstPaymentDate)
                .autoPayment(true)
                .nextAutoPaymentDate(nextAutoPayment)
                .build();
    }

    // ─── processReminders (reminder flow) ─────────────────────────────────────

    @Nested
    @DisplayName("processReminders — reminder flow")
    class ProcessReminderFlow {

        @BeforeEach
        void stubBase() {
            lenient().when(debtRS.findDueAutoPayments(any())).thenReturn(List.of());
            lenient().when(debtRS.findActiveDebts()).thenReturn(List.of());
            lenient().when(debtRS.findUnpaidManualDueDebts(any())).thenReturn(List.of());
        }

        @Test
        @DisplayName("no due reminders → early return, no events produced")
        void noDueReminders_earlyReturn() {
            when(debtRS.findDueReminders(any())).thenReturn(List.of());

            scheduler.processReminders();

            verifyNoInteractions(debtEventProducer);
            verify(debtRS, never()).update(any());
        }

        @Test
        @DisplayName("due reminder debt → event produced and debt updated")
        void dueReminder_producesEventAndUpdates() {
            final DebtDocument debt = buildReminderDebt("debt1", PaymentPeriodType.MONTHLY, BASE_TIME, BASE_TIME);
            when(debtRS.findDueReminders(any())).thenReturn(List.of(debt));
            when(debtRS.update(any())).thenReturn(debt);

            scheduler.processReminders();

            verify(debtEventProducer).produce(anyString(), anyString(), any());
            verify(debtRS).update(debt);
        }

        @Test
        @DisplayName("publish fails → DB is still updated, no exception propagated")
        void publishFails_dbStillUpdated() {
            final DebtDocument debt = buildReminderDebt("debt1", PaymentPeriodType.MONTHLY, BASE_TIME, BASE_TIME);
            when(debtRS.findDueReminders(any())).thenReturn(List.of(debt));
            when(debtRS.update(any())).thenReturn(debt);
            doThrow(new RuntimeException("RabbitMQ down"))
                    .when(debtEventProducer).produce(anyString(), anyString(), any());

            assertThatNoException().isThrownBy(() -> scheduler.processReminders());

            verify(debtRS).update(debt);
        }

        @Test
        @DisplayName("publish succeeds but DB update fails → no exception propagated")
        void dbUpdateFails_noExceptionPropagated() {
            final DebtDocument debt = buildReminderDebt("debt1", PaymentPeriodType.MONTHLY, BASE_TIME, BASE_TIME);
            when(debtRS.findDueReminders(any())).thenReturn(List.of(debt));
            when(debtRS.update(any())).thenThrow(new RuntimeException("DB error"));

            assertThatNoException().isThrownBy(() -> scheduler.processReminders());
        }

        @Test
        @DisplayName("multiple debts — one fails publish, other succeeds → both debts updated in DB")
        void multipleDebts_partialFailure() {
            final DebtDocument debt1 = buildReminderDebt("debt1", PaymentPeriodType.MONTHLY, BASE_TIME, BASE_TIME);
            final DebtDocument debt2 = buildReminderDebt("debt2", PaymentPeriodType.MONTHLY, BASE_TIME, BASE_TIME);
            when(debtRS.findDueReminders(any())).thenReturn(List.of(debt1, debt2));
            when(debtRS.update(any())).thenReturn(debt1);

            doThrow(new RuntimeException("fail"))
                    .when(debtEventProducer).produce(anyString(), anyString(),
                            argThat(e -> {
                                if (e instanceof dev.batuhanyetgin.commonmodule.event.DebtReminderEvent evt) {
                                    return "debt1".equals(evt.getDebtId());
                                }
                                return false;
                            }));

            assertThatNoException().isThrownBy(() -> scheduler.processReminders());

            verify(debtRS, times(2)).update(any());
        }
    }

    // ─── processReminders (auto-payment flow) ────────────────────────────────

    @Nested
    @DisplayName("processReminders — auto-payment flow")
    class ProcessAutoPaymentFlow {

        @BeforeEach
        void stubReminderEmpty() {
            when(debtRS.findDueReminders(any())).thenReturn(List.of());
        }

        @Test
        @DisplayName("no due auto-payments → early return, no events, no updates")
        void noDueAutoPayments_earlyReturn() {
            when(debtRS.findDueAutoPayments(any())).thenReturn(List.of());

            scheduler.processReminders();

            verifyNoInteractions(debtEventProducer);
            verify(debtRS, never()).update(any());
        }

        @Test
        @DisplayName("due auto-payment debt → payment recorded, no reminder event produced")
        void dueAutoPayment_recordsPaymentNoEvent() {
            final DebtDocument debt = buildAutoPaymentDebt("debt1", PaymentPeriodType.MONTHLY, BASE_TIME, BASE_TIME);
            when(debtRS.findDueAutoPayments(any())).thenReturn(List.of(debt));

            final ArgumentCaptor<DebtDocument> captor = ArgumentCaptor.forClass(DebtDocument.class);
            when(debtRS.update(captor.capture())).thenReturn(debt);

            scheduler.processReminders();

            verifyNoInteractions(debtEventProducer);
            assertThat(captor.getValue().getPayments()).hasSize(1);
            assertThat(captor.getValue().getPayments().get(0).isPaid()).isTrue();
        }

        @Test
        @DisplayName("due auto-payment debt → next auto-payment date advanced")
        void dueAutoPayment_dateAdvanced() {
            final DebtDocument debt = buildAutoPaymentDebt("debt1", PaymentPeriodType.MONTHLY, BASE_TIME, BASE_TIME);
            when(debtRS.findDueAutoPayments(any())).thenReturn(List.of(debt));

            final ArgumentCaptor<DebtDocument> captor = ArgumentCaptor.forClass(DebtDocument.class);
            when(debtRS.update(captor.capture())).thenReturn(debt);

            scheduler.processReminders();

            assertThat(captor.getValue().getNextAutoPaymentDate())
                    .isEqualTo(BASE_TIME.plusMonths(1));
        }

        @Test
        @DisplayName("DB update fails → no exception propagated")
        void dbUpdateFails_noExceptionPropagated() {
            final DebtDocument debt = buildAutoPaymentDebt("debt1", PaymentPeriodType.MONTHLY, BASE_TIME, BASE_TIME);
            when(debtRS.findDueAutoPayments(any())).thenReturn(List.of(debt));
            when(debtRS.update(any())).thenThrow(new RuntimeException("DB error"));

            assertThatNoException().isThrownBy(() -> scheduler.processReminders());
        }

        @Test
        @DisplayName("installmentCount reached → debt marked COMPLETED, paymentStatus PAID, nextAutoPaymentDate not advanced")
        void installmentCountReached_markedCompletedAndPaid() {
            final DebtDocument debt = DebtDocument.builder()
                    .id("debt1").ownerId("owner1").name("Auto Debt")
                    .amount(java.math.BigDecimal.TEN)
                    .paymentPeriodType(PaymentPeriodType.MONTHLY)
                    .firstPaymentDate(LocalDateTime.now().minusMonths(2))
                    .installmentCount(1)
                    .autoPayment(true)
                    .nextAutoPaymentDate(BASE_TIME)
                    .build();
            when(debtRS.findDueAutoPayments(any())).thenReturn(List.of(debt));

            final ArgumentCaptor<DebtDocument> captor = ArgumentCaptor.forClass(DebtDocument.class);
            when(debtRS.update(captor.capture())).thenReturn(debt);

            scheduler.processReminders();

            assertThat(captor.getValue().getStatus()).isEqualTo(DebtStatus.COMPLETED);
            assertThat(captor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(captor.getValue().getNextAutoPaymentDate()).isEqualTo(BASE_TIME); // not advanced
        }

        @Test
        @DisplayName("installmentCount not yet reached → nextAutoPaymentDate advanced, status remains ACTIVE")
        void installmentCountNotReached_advancedAndActive() {
            final DebtDocument debt = DebtDocument.builder()
                    .id("debt1").ownerId("owner1").name("Auto Debt")
                    .amount(java.math.BigDecimal.TEN)
                    .paymentPeriodType(PaymentPeriodType.MONTHLY)
                    .firstPaymentDate(BASE_TIME)
                    .installmentCount(6)
                    .autoPayment(true)
                    .nextAutoPaymentDate(BASE_TIME)
                    .build();
            when(debtRS.findDueAutoPayments(any())).thenReturn(List.of(debt));

            final ArgumentCaptor<DebtDocument> captor = ArgumentCaptor.forClass(DebtDocument.class);
            when(debtRS.update(captor.capture())).thenReturn(debt);

            scheduler.processReminders();

            assertThat(captor.getValue().getStatus()).isEqualTo(DebtStatus.ACTIVE);
            assertThat(captor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(captor.getValue().getNextAutoPaymentDate()).isEqualTo(BASE_TIME.plusMonths(1));
        }
    }

    // ─── processReminders (overdue flow) ────────────────────────────────────

    @Nested
    @DisplayName("processReminders — overdue flow")
    class ProcessOverdueFlow {

        @BeforeEach
        void stubBase() {
            lenient().when(debtRS.findDueAutoPayments(any())).thenReturn(List.of());
            lenient().when(debtRS.findActiveDebts()).thenReturn(List.of());
            lenient().when(debtRS.findDueReminders(any())).thenReturn(List.of());
        }

        @Test
        @DisplayName("unpaid manual debt past due date → paymentStatus set to OVERDUE")
        void unpaidManualDebt_markedOverdue() {
            final DebtDocument debt = buildReminderDebt("debt1", PaymentPeriodType.MONTHLY, BASE_TIME.minusDays(5), BASE_TIME);
            debt.setPaymentStatus(PaymentStatus.UNPAID);
            debt.setAutoPayment(false);
            
            when(debtRS.findUnpaidManualDueDebts(any())).thenReturn(List.of(debt));
            final ArgumentCaptor<DebtDocument> captor = ArgumentCaptor.forClass(DebtDocument.class);
            when(debtRS.update(captor.capture())).thenReturn(debt);

            scheduler.processReminders();

            assertThat(captor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.OVERDUE);
            assertThat(captor.getValue().getStatus()).isEqualTo(DebtStatus.ACTIVE);
        }
    }

    // ─── processReminders (status reset flow) ────────────────────────────────

    @Nested
    @DisplayName("processReminders — status reset flow")
    class ProcessStatusResetFlow {

        @BeforeEach
        void stubBase() {
            lenient().when(debtRS.findDueAutoPayments(any())).thenReturn(List.of());
            lenient().when(debtRS.findDueReminders(any())).thenReturn(List.of());
            lenient().when(debtRS.findUnpaidManualDueDebts(any())).thenReturn(List.of());
        }

        @Test
        @DisplayName("ACTIVE debt with PAID status from previous period → paymentStatus reset to UNPAID")
        void paidDebtInNewPeriod_resetToUnpaid() {
            // Debt was paid last month
            final LocalDateTime lastMonth = BASE_TIME.minusMonths(1);
            final DebtDocument debt = buildReminderDebt("debt1", PaymentPeriodType.MONTHLY, lastMonth, BASE_TIME);
            debt.setPaymentStatus(PaymentStatus.PAID);
            debt.addPayment(lastMonth, lastMonth, lastMonth.plusDays(30), BigDecimal.TEN);

            when(debtRS.findActiveDebts()).thenReturn(List.of(debt));
            final ArgumentCaptor<DebtDocument> captor = ArgumentCaptor.forClass(DebtDocument.class);
            when(debtRS.update(captor.capture())).thenReturn(debt);

            scheduler.processReminders();

            assertThat(captor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        }
    }

    // ─── advanceNextReminderDate ──────────────────────────────────────────────

    @Nested
    @DisplayName("advanceNextReminderDate")
    class AdvanceNextReminderDate {

        @BeforeEach
        void stubBase() {
            lenient().when(debtRS.findDueAutoPayments(any())).thenReturn(List.of());
            lenient().when(debtRS.findActiveDebts()).thenReturn(List.of());
            lenient().when(debtRS.findUnpaidManualDueDebts(any())).thenReturn(List.of());
        }

        private LocalDateTime captureNextReminderDate(final PaymentPeriodType periodType,
                                                      final LocalDateTime firstPaymentDate,
                                                      final LocalDateTime nextReminder) {
            final DebtDocument debt = buildReminderDebt("debt1", periodType, firstPaymentDate, nextReminder);
            when(debtRS.findDueReminders(any())).thenReturn(List.of(debt));

            final ArgumentCaptor<DebtDocument> captor = ArgumentCaptor.forClass(DebtDocument.class);
            when(debtRS.update(captor.capture())).thenReturn(debt);

            scheduler.processReminders();

            return captor.getValue().getNextReminderDate();
        }

        @Test
        @DisplayName("DAILY → advances by 1 day")
        void daily_advancesOneDay() {
            assertThat(captureNextReminderDate(PaymentPeriodType.DAILY, BASE_TIME, BASE_TIME))
                    .isEqualTo(BASE_TIME.plusDays(1));
        }

        @Test
        @DisplayName("WEEKLY → advances by 1 week")
        void weekly_advancesOneWeek() {
            assertThat(captureNextReminderDate(PaymentPeriodType.WEEKLY, BASE_TIME, BASE_TIME))
                    .isEqualTo(BASE_TIME.plusWeeks(1));
        }

        @Test
        @DisplayName("MONTHLY from day-1 date → advances by 1 month")
        void monthly_advancesOneMonth() {
            assertThat(captureNextReminderDate(PaymentPeriodType.MONTHLY, BASE_TIME, BASE_TIME))
                    .isEqualTo(BASE_TIME.plusMonths(1));
        }

        @Test
        @DisplayName("HALF_YEARLY → advances by 6 months")
        void halfYearly_advancesSixMonths() {
            assertThat(captureNextReminderDate(PaymentPeriodType.HALF_YEARLY, BASE_TIME, BASE_TIME))
                    .isEqualTo(BASE_TIME.plusMonths(6));
        }

        @Test
        @DisplayName("YEARLY → advances by 1 year")
        void yearly_advancesOneYear() {
            assertThat(captureNextReminderDate(PaymentPeriodType.YEARLY, BASE_TIME, BASE_TIME))
                    .isEqualTo(BASE_TIME.plusYears(1));
        }

        @Test
        @DisplayName("MONTHLY from Jan-31 → clamps to Feb-28 (short month)")
        void monthly_jan31_clampsToFeb28() {
            final LocalDateTime result = captureNextReminderDate(PaymentPeriodType.MONTHLY, JAN_31, JAN_31);
            assertThat(result).isEqualTo(LocalDateTime.of(2026, 2, 28, 10, 0));
        }

        @Test
        @DisplayName("MONTHLY from Feb-28 (originalDay=31) → recovers to Mar-31")
        void monthly_feb28_originalDay31_recoversMar31() {
            final LocalDateTime feb28 = LocalDateTime.of(2026, 2, 28, 10, 0);
            final LocalDateTime result = captureNextReminderDate(PaymentPeriodType.MONTHLY, JAN_31, feb28);
            assertThat(result).isEqualTo(LocalDateTime.of(2026, 3, 31, 10, 0));
        }
    }

    // ─── advanceNextAutoPaymentDate ───────────────────────────────────────────

    @Nested
    @DisplayName("advanceNextAutoPaymentDate")
    class AdvanceNextAutoPaymentDate {

        @BeforeEach
        void stubReminderEmpty() {
            when(debtRS.findDueReminders(any())).thenReturn(List.of());
        }

        private LocalDateTime captureNextAutoPaymentDate(final PaymentPeriodType periodType,
                                                         final LocalDateTime firstPaymentDate,
                                                         final LocalDateTime nextAutoPayment) {
            final DebtDocument debt = buildAutoPaymentDebt("debt1", periodType, firstPaymentDate, nextAutoPayment);
            when(debtRS.findDueAutoPayments(any())).thenReturn(List.of(debt));

            final ArgumentCaptor<DebtDocument> captor = ArgumentCaptor.forClass(DebtDocument.class);
            when(debtRS.update(captor.capture())).thenReturn(debt);

            scheduler.processReminders();

            return captor.getValue().getNextAutoPaymentDate();
        }

        @Test
        @DisplayName("DAILY → advances by 1 day")
        void daily_advancesOneDay() {
            assertThat(captureNextAutoPaymentDate(PaymentPeriodType.DAILY, BASE_TIME, BASE_TIME))
                    .isEqualTo(BASE_TIME.plusDays(1));
        }

        @Test
        @DisplayName("WEEKLY → advances by 1 week")
        void weekly_advancesOneWeek() {
            assertThat(captureNextAutoPaymentDate(PaymentPeriodType.WEEKLY, BASE_TIME, BASE_TIME))
                    .isEqualTo(BASE_TIME.plusWeeks(1));
        }

        @Test
        @DisplayName("MONTHLY from day-1 date → advances by 1 month")
        void monthly_advancesOneMonth() {
            assertThat(captureNextAutoPaymentDate(PaymentPeriodType.MONTHLY, BASE_TIME, BASE_TIME))
                    .isEqualTo(BASE_TIME.plusMonths(1));
        }

        @Test
        @DisplayName("HALF_YEARLY → advances by 6 months")
        void halfYearly_advancesSixMonths() {
            assertThat(captureNextAutoPaymentDate(PaymentPeriodType.HALF_YEARLY, BASE_TIME, BASE_TIME))
                    .isEqualTo(BASE_TIME.plusMonths(6));
        }

        @Test
        @DisplayName("YEARLY → advances by 1 year")
        void yearly_advancesOneYear() {
            assertThat(captureNextAutoPaymentDate(PaymentPeriodType.YEARLY, BASE_TIME, BASE_TIME))
                    .isEqualTo(BASE_TIME.plusYears(1));
        }

        @Test
        @DisplayName("MONTHLY from Jan-31 → clamps to Feb-28 (short month)")
        void monthly_jan31_clampsToFeb28() {
            final LocalDateTime result = captureNextAutoPaymentDate(PaymentPeriodType.MONTHLY, JAN_31, JAN_31);
            assertThat(result).isEqualTo(LocalDateTime.of(2026, 2, 28, 10, 0));
        }

        @Test
        @DisplayName("MONTHLY from Feb-28 (originalDay=31) → recovers to Mar-31")
        void monthly_feb28_originalDay31_recoversMar31() {
            final LocalDateTime feb28 = LocalDateTime.of(2026, 2, 28, 10, 0);
            final LocalDateTime result = captureNextAutoPaymentDate(PaymentPeriodType.MONTHLY, JAN_31, feb28);
            assertThat(result).isEqualTo(LocalDateTime.of(2026, 3, 31, 10, 0));
        }
    }
}
