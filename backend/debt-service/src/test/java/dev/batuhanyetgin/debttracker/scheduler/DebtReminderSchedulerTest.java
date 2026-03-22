package dev.batuhanyetgin.debttracker.scheduler;

import dev.batuhanyetgin.commonmodule.event.ReminderChannelType;
import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.document.embedded.ReminderEmbedded;
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

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 4, 1, 10, 0);

    @BeforeEach
    void setUp() {
        scheduler = new DebtReminderScheduler(debtRS, debtEventProducer, taskExecutor);
    }

    private DebtDocument buildDebt(final String id, final PaymentPeriodType periodType,
                                   final LocalDateTime nextReminder) {
        return DebtDocument.builder()
                .id(id)
                .ownerId("owner1")
                .name("Test Debt")
                .amount(BigDecimal.TEN)
                .paymentPeriodType(periodType)
                .nextReminderDate(nextReminder)
                .reminder(ReminderEmbedded.builder()
                        .enabled(true)
                        .channels(List.of(ReminderChannelType.IN_APP))
                        .build())
                .build();
    }

    // ─── processReminders ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("processReminders")
    class ProcessReminders {

        @Test
        @DisplayName("no due debts → early return, no events produced")
        void noDueDebts_earlyReturn() {
            when(debtRS.findDueReminders(any())).thenReturn(List.of());

            scheduler.processReminders();

            verifyNoInteractions(debtEventProducer);
            verify(debtRS, never()).update(any());
        }

        @Test
        @DisplayName("due debts → event produced and debt updated for each")
        void dueDebts_producesEventAndUpdates() {
            final DebtDocument debt = buildDebt("debt1", PaymentPeriodType.MONTHLY, BASE_TIME);
            when(debtRS.findDueReminders(any())).thenReturn(List.of(debt));
            when(debtRS.update(any())).thenReturn(debt);

            scheduler.processReminders();

            verify(debtEventProducer).produce(anyString(), anyString(), any());
            verify(debtRS).update(debt);
        }

        @Test
        @DisplayName("publish fails → no DB update for that debt")
        void publishFails_noDbUpdate() {
            final DebtDocument debt = buildDebt("debt1", PaymentPeriodType.MONTHLY, BASE_TIME);
            when(debtRS.findDueReminders(any())).thenReturn(List.of(debt));
            doThrow(new RuntimeException("RabbitMQ down"))
                    .when(debtEventProducer).produce(anyString(), anyString(), any());

            assertThatNoException().isThrownBy(() -> scheduler.processReminders());

            verify(debtRS, never()).update(any());
        }

        @Test
        @DisplayName("publish succeeds but DB update fails → no exception propagated")
        void dbUpdateFails_noExceptionPropagated() {
            final DebtDocument debt = buildDebt("debt1", PaymentPeriodType.MONTHLY, BASE_TIME);
            when(debtRS.findDueReminders(any())).thenReturn(List.of(debt));
            when(debtRS.update(any())).thenThrow(new RuntimeException("DB error"));

            assertThatNoException().isThrownBy(() -> scheduler.processReminders());
        }

        @Test
        @DisplayName("multiple debts — one fails publish, one succeeds → only successful debt updated")
        void multipleDebts_partialFailure() {
            final DebtDocument debt1 = buildDebt("debt1", PaymentPeriodType.MONTHLY, BASE_TIME);
            final DebtDocument debt2 = buildDebt("debt2", PaymentPeriodType.MONTHLY, BASE_TIME);
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

            verify(debtRS, times(1)).update(any());
        }
    }

    // ─── advanceNextReminderDate (tested via processReminders) ────────────────

    @Nested
    @DisplayName("advanceNextReminderDate")
    class AdvanceNextReminderDate {

        private LocalDateTime captureNextReminderDate(final PaymentPeriodType periodType) {
            final DebtDocument debt = buildDebt("debt1", periodType, BASE_TIME);
            when(debtRS.findDueReminders(any())).thenReturn(List.of(debt));

            final ArgumentCaptor<DebtDocument> captor = ArgumentCaptor.forClass(DebtDocument.class);
            when(debtRS.update(captor.capture())).thenReturn(debt);

            scheduler.processReminders();

            return captor.getValue().getNextReminderDate();
        }

        @Test
        @DisplayName("DAILY → advances by 1 day")
        void daily_advancesOneDay() {
            assertThat(captureNextReminderDate(PaymentPeriodType.DAILY))
                    .isEqualTo(BASE_TIME.plusDays(1));
        }

        @Test
        @DisplayName("WEEKLY → advances by 1 week")
        void weekly_advancesOneWeek() {
            assertThat(captureNextReminderDate(PaymentPeriodType.WEEKLY))
                    .isEqualTo(BASE_TIME.plusWeeks(1));
        }

        @Test
        @DisplayName("MONTHLY → advances by 1 month")
        void monthly_advancesOneMonth() {
            assertThat(captureNextReminderDate(PaymentPeriodType.MONTHLY))
                    .isEqualTo(BASE_TIME.plusMonths(1));
        }

        @Test
        @DisplayName("HALF_YEARLY → advances by 6 months")
        void halfYearly_advancesSixMonths() {
            assertThat(captureNextReminderDate(PaymentPeriodType.HALF_YEARLY))
                    .isEqualTo(BASE_TIME.plusMonths(6));
        }

        @Test
        @DisplayName("YEARLY → advances by 1 year")
        void yearly_advancesOneYear() {
            assertThat(captureNextReminderDate(PaymentPeriodType.YEARLY))
                    .isEqualTo(BASE_TIME.plusYears(1));
        }
    }
}