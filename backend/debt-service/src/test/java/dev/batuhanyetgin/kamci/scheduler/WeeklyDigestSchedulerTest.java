package dev.batuhanyetgin.kamci.scheduler;

import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.commonmodule.event.WeeklyDigestEvent;
import dev.batuhanyetgin.kamci.document.DebtDocument;
import dev.batuhanyetgin.kamci.enums.PaymentPeriodType;
import dev.batuhanyetgin.kamci.producer.DebtEventProducer;
import dev.batuhanyetgin.kamci.repositoryservice.DebtRepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeeklyDigestSchedulerTest {

    // Monday 09:00 — matches the scheduler's cron intent
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 4, 6, 9, 0);
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private DebtRepositoryService debtRS;
    @Mock
    private DebtEventProducer debtEventProducer;

    private WeeklyDigestScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new WeeklyDigestScheduler(debtRS, debtEventProducer, FIXED_CLOCK);
    }

    private DebtDocument buildDebt(final String id, final String ownerId, final PaymentPeriodType periodType,
                                   final LocalDateTime firstPaymentDate, final BigDecimal amount) {
        return DebtDocument.builder()
                .id(id)
                .ownerId(ownerId)
                .name("Debt " + id)
                .amount(amount)
                .paymentPeriodType(periodType)
                .firstPaymentDate(firstPaymentDate)
                .build();
    }

    @Test
    @DisplayName("no active debts → no events produced")
    void noActiveDebts_noEvents() {
        when(debtRS.findActiveDebts()).thenReturn(List.of());

        scheduler.processWeeklyDigest();

        verifyNoInteractions(debtEventProducer);
    }

    @Test
    @DisplayName("firstPaymentDate null → excluded, no event")
    void nullFirstPaymentDate_excluded() {
        final DebtDocument debt = DebtDocument.builder()
                .id("d1").ownerId("owner1").name("Debt").amount(BigDecimal.TEN)
                .paymentPeriodType(PaymentPeriodType.MONTHLY)
                .firstPaymentDate(null)
                .build();
        when(debtRS.findActiveDebts()).thenReturn(List.of(debt));

        scheduler.processWeeklyDigest();

        verifyNoInteractions(debtEventProducer);
    }

    @Test
    @DisplayName("due date within the 7-day window and unpaid → event produced")
    void dueWithinWindow_unpaid_eventProduced() {
        // WEEKLY, started 6 days ago → current period ends tomorrow, inside the window
        final DebtDocument debt = buildDebt("d1", "owner1", PaymentPeriodType.WEEKLY, NOW.minusDays(6), BigDecimal.valueOf(100));
        when(debtRS.findActiveDebts()).thenReturn(List.of(debt));

        scheduler.processWeeklyDigest();

        final ArgumentCaptor<WeeklyDigestEvent> captor = ArgumentCaptor.forClass(WeeklyDigestEvent.class);
        verify(debtEventProducer).produce(eq(RabbitMQConstants.NOTIFICATION_EXCHANGE),
                eq(RabbitMQConstants.WEEKLY_DIGEST_ROUTING_KEY), captor.capture());

        final WeeklyDigestEvent event = captor.getValue();
        assertThat(event.getOwnerId()).isEqualTo("owner1");
        assertThat(event.getItems()).hasSize(1);
        assertThat(event.getItems().get(0).getDebtId()).isEqualTo("d1");
        assertThat(event.getTotalAmount()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("ONE_TIME debt due inside window → included with dueDate = firstPaymentDate")
    void oneTime_dueInsideWindow_included() {
        final LocalDateTime dueInThreeDays = NOW.plusDays(3);
        final DebtDocument debt = buildDebt("d1", "owner1", PaymentPeriodType.ONE_TIME, dueInThreeDays, BigDecimal.TEN);
        when(debtRS.findActiveDebts()).thenReturn(List.of(debt));

        scheduler.processWeeklyDigest();

        final ArgumentCaptor<WeeklyDigestEvent> captor = ArgumentCaptor.forClass(WeeklyDigestEvent.class);
        verify(debtEventProducer).produce(anyString(), anyString(), captor.capture());
        assertThat(captor.getValue().getItems().get(0).getDueDate()).isEqualTo(dueInThreeDays);
    }

    @Test
    @DisplayName("due date already past (overdue) → excluded")
    void dueDateBeforeNow_excluded() {
        // MONTHLY with firstPaymentDate far in the past whose current period already ended before now
        final DebtDocument debt = buildDebt("d1", "owner1", PaymentPeriodType.ONE_TIME, NOW.minusDays(1), BigDecimal.TEN);
        when(debtRS.findActiveDebts()).thenReturn(List.of(debt));

        scheduler.processWeeklyDigest();

        verifyNoInteractions(debtEventProducer);
    }

    @Test
    @DisplayName("due date beyond the 7-day window → excluded")
    void dueDateAfterWindow_excluded() {
        final DebtDocument debt = buildDebt("d1", "owner1", PaymentPeriodType.ONE_TIME, NOW.plusDays(8), BigDecimal.TEN);
        when(debtRS.findActiveDebts()).thenReturn(List.of(debt));

        scheduler.processWeeklyDigest();

        verifyNoInteractions(debtEventProducer);
    }

    @Test
    @DisplayName("current period already paid → excluded")
    void currentPeriodAlreadyPaid_excluded() {
        // same within-window MONTHLY setup as priorPeriodPaid_currentUnpaid_included, but paid for the current period
        final LocalDateTime firstPayment = NOW.minusDays(25);
        final DebtDocument debt = buildDebt("d1", "owner1", PaymentPeriodType.MONTHLY, firstPayment, BigDecimal.TEN);
        debt.addPayment(NOW.minusDays(1), firstPayment, NOW.plusDays(5), BigDecimal.TEN);
        when(debtRS.findActiveDebts()).thenReturn(List.of(debt));

        scheduler.processWeeklyDigest();

        verifyNoInteractions(debtEventProducer);
    }

    @Test
    @DisplayName("current period unpaid despite past payments from prior periods → included")
    void priorPeriodPaid_currentUnpaid_included() {
        // MONTHLY, current period started 25 days ago → ends in 5 days, inside the window
        final LocalDateTime firstPayment = NOW.minusDays(25);
        final DebtDocument debt = buildDebt("d1", "owner1", PaymentPeriodType.MONTHLY, firstPayment, BigDecimal.TEN);
        // paid for the previous period only (well before the current period start), not the current one
        debt.addPayment(firstPayment.minusMonths(1), firstPayment.minusMonths(1), firstPayment, BigDecimal.TEN);
        when(debtRS.findActiveDebts()).thenReturn(List.of(debt));

        scheduler.processWeeklyDigest();

        verify(debtEventProducer).produce(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("multiple debts, same owner → grouped into one event, sorted by due date, amounts summed")
    void multipleDebtsSameOwner_groupedAndSummed() {
        final DebtDocument later = buildDebt("later", "owner1", PaymentPeriodType.ONE_TIME, NOW.plusDays(5), BigDecimal.valueOf(50));
        final DebtDocument earlier = buildDebt("earlier", "owner1", PaymentPeriodType.ONE_TIME, NOW.plusDays(1), BigDecimal.valueOf(30));
        when(debtRS.findActiveDebts()).thenReturn(List.of(later, earlier));

        scheduler.processWeeklyDigest();

        final ArgumentCaptor<WeeklyDigestEvent> captor = ArgumentCaptor.forClass(WeeklyDigestEvent.class);
        verify(debtEventProducer, times(1)).produce(anyString(), anyString(), captor.capture());

        final WeeklyDigestEvent event = captor.getValue();
        assertThat(event.getItems()).hasSize(2);
        assertThat(event.getItems().get(0).getDebtId()).isEqualTo("earlier");
        assertThat(event.getItems().get(1).getDebtId()).isEqualTo("later");
        assertThat(event.getTotalAmount()).isEqualByComparingTo("80");
    }

    @Test
    @DisplayName("multiple owners → one event produced per owner")
    void multipleOwners_oneEventEach() {
        final DebtDocument debtOwner1 = buildDebt("d1", "owner1", PaymentPeriodType.ONE_TIME, NOW.plusDays(1), BigDecimal.TEN);
        final DebtDocument debtOwner2 = buildDebt("d2", "owner2", PaymentPeriodType.ONE_TIME, NOW.plusDays(1), BigDecimal.TEN);
        when(debtRS.findActiveDebts()).thenReturn(List.of(debtOwner1, debtOwner2));

        scheduler.processWeeklyDigest();

        verify(debtEventProducer, times(2)).produce(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("produce throws for one owner → exception swallowed, other owner still processed")
    void produceFailsForOneOwner_othersStillProcessed() {
        final DebtDocument debtOwner1 = buildDebt("d1", "owner1", PaymentPeriodType.ONE_TIME, NOW.plusDays(1), BigDecimal.TEN);
        final DebtDocument debtOwner2 = buildDebt("d2", "owner2", PaymentPeriodType.ONE_TIME, NOW.plusDays(1), BigDecimal.TEN);
        when(debtRS.findActiveDebts()).thenReturn(List.of(debtOwner1, debtOwner2));

        doThrow(new RuntimeException("RabbitMQ down"))
                .when(debtEventProducer).produce(anyString(), anyString(),
                        argThat(e -> e instanceof WeeklyDigestEvent evt && "owner1".equals(evt.getOwnerId())));

        assertThatNoException().isThrownBy(() -> scheduler.processWeeklyDigest());

        verify(debtEventProducer, times(2)).produce(anyString(), anyString(), any());
    }

    @Nested
    @DisplayName("default (Autowired) constructor")
    class DefaultConstructor {

        @Test
        @DisplayName("JVM default timezone is UTC → scheduler still clocks 'now' in Europe/Istanbul")
        void jvmDefaultTimezoneUtc_clockStillReadsIstanbulTime() {
            // Same root cause as the payment_before_start bug: Clock.systemDefaultZone() in the
            // @Autowired constructor followed the JVM's zone (UTC in prod), not Turkey wall-clock time.
            final TimeZone originalDefault = TimeZone.getDefault();
            try {
                TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

                final WeeklyDigestScheduler prodScheduler =
                        new WeeklyDigestScheduler(debtRS, debtEventProducer);
                final Clock injectedClock = (Clock) org.springframework.test.util.ReflectionTestUtils
                        .getField(prodScheduler, "clock");

                final LocalDateTime schedulerNow = LocalDateTime.now(injectedClock);
                final LocalDateTime istanbulNow = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));

                assertThat(Duration.between(schedulerNow, istanbulNow).abs()).isLessThan(Duration.ofSeconds(5));
            } finally {
                TimeZone.setDefault(originalDefault);
            }
        }
    }
}
