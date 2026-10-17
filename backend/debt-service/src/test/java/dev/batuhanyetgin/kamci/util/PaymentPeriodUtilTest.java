package dev.batuhanyetgin.kamci.util;

import dev.batuhanyetgin.kamci.enums.PaymentPeriodType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class PaymentPeriodUtilTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2025, 1, 1, 10, 0, 0);

    // ─── getPeriodStart ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPeriodStart")
    class GetPeriodStart {

        @Test
        @DisplayName("null firstPaymentDate → returns now")
        void nullFirstPaymentDate_returnsNow() {
            final LocalDateTime now = LocalDateTime.now();
            final LocalDateTime result = PaymentPeriodUtil.getPeriodStart(null, PaymentPeriodType.MONTHLY, now);
            assertThat(result).isEqualTo(now);
        }

        @Test
        @DisplayName("firstPaymentDate in the future → returns now")
        void futureFirstPaymentDate_returnsNow() {
            final LocalDateTime now = LocalDateTime.now();
            final LocalDateTime future = now.plusDays(5);
            final LocalDateTime result = PaymentPeriodUtil.getPeriodStart(future, PaymentPeriodType.MONTHLY, now);
            assertThat(result).isEqualTo(now);
        }

        @Test
        @DisplayName("ONE_TIME → returns firstPaymentDate")
        void oneTime_returnsFirstPaymentDate() {
            final LocalDateTime now = BASE.plusMonths(3);
            final LocalDateTime result = PaymentPeriodUtil.getPeriodStart(BASE, PaymentPeriodType.ONE_TIME, now);
            assertThat(result).isEqualTo(BASE);
        }

        @Test
        @DisplayName("DAILY → returns start of current day period")
        void daily_returnsCurrentDayPeriodStart() {
            final LocalDateTime now = BASE.plusDays(5);
            final LocalDateTime result = PaymentPeriodUtil.getPeriodStart(BASE, PaymentPeriodType.DAILY, now);
            assertThat(result).isEqualTo(BASE.plusDays(5));
        }

        @Test
        @DisplayName("WEEKLY → returns start of current week period")
        void weekly_returnsCurrentWeekPeriodStart() {
            final LocalDateTime now = BASE.plusWeeks(3).plusDays(2);
            final LocalDateTime result = PaymentPeriodUtil.getPeriodStart(BASE, PaymentPeriodType.WEEKLY, now);
            assertThat(result).isEqualTo(BASE.plusWeeks(3));
        }

        @Test
        @DisplayName("BI_WEEKLY → returns start of current bi-weekly period")
        void biWeekly_returnsCurrentBiWeekPeriodStart() {
            final LocalDateTime now = BASE.plusWeeks(5);
            final LocalDateTime result = PaymentPeriodUtil.getPeriodStart(BASE, PaymentPeriodType.BI_WEEKLY, now);
            assertThat(result).isEqualTo(BASE.plusWeeks(4));
        }

        @Test
        @DisplayName("MONTHLY → returns start of current month period")
        void monthly_returnsCurrentMonthPeriodStart() {
            final LocalDateTime now = BASE.plusMonths(2).plusDays(10);
            final LocalDateTime result = PaymentPeriodUtil.getPeriodStart(BASE, PaymentPeriodType.MONTHLY, now);
            assertThat(result).isEqualTo(BASE.plusMonths(2));
        }

        @Test
        @DisplayName("QUARTERLY → returns start of current quarter period")
        void quarterly_returnsCurrentQuarterPeriodStart() {
            final LocalDateTime now = BASE.plusMonths(7);
            final LocalDateTime result = PaymentPeriodUtil.getPeriodStart(BASE, PaymentPeriodType.QUARTERLY, now);
            assertThat(result).isEqualTo(BASE.plusMonths(6));
        }

        @Test
        @DisplayName("HALF_YEARLY → returns start of current half-year period")
        void halfYearly_returnsCurrentHalfYearPeriodStart() {
            final LocalDateTime now = BASE.plusMonths(8);
            final LocalDateTime result = PaymentPeriodUtil.getPeriodStart(BASE, PaymentPeriodType.HALF_YEARLY, now);
            assertThat(result).isEqualTo(BASE.plusMonths(6));
        }

        @Test
        @DisplayName("YEARLY → returns start of current year period")
        void yearly_returnsCurrentYearPeriodStart() {
            final LocalDateTime now = BASE.plusYears(2).plusMonths(3);
            final LocalDateTime result = PaymentPeriodUtil.getPeriodStart(BASE, PaymentPeriodType.YEARLY, now);
            assertThat(result).isEqualTo(BASE.plusYears(2));
        }

        @Test
        @DisplayName("MONTHLY, firstPaymentDate == now → returns firstPaymentDate")
        void monthly_firstPaymentDateEqualsNow_returnsFirstPaymentDate() {
            final LocalDateTime result = PaymentPeriodUtil.getPeriodStart(BASE, PaymentPeriodType.MONTHLY, BASE);
            assertThat(result).isEqualTo(BASE);
        }
    }

    // ─── advanceDate ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("advanceDate")
    class AdvanceDate {

        @Test
        @DisplayName("ONE_TIME → returns null")
        void oneTime_returnsNull() {
            assertThat(PaymentPeriodUtil.advanceDate(BASE, PaymentPeriodType.ONE_TIME, 1)).isNull();
        }

        @Test
        @DisplayName("DAILY → adds 1 day")
        void daily_addsOneDay() {
            assertThat(PaymentPeriodUtil.advanceDate(BASE, PaymentPeriodType.DAILY, 1))
                    .isEqualTo(BASE.plusDays(1));
        }

        @Test
        @DisplayName("WEEKLY → adds 1 week")
        void weekly_addsOneWeek() {
            assertThat(PaymentPeriodUtil.advanceDate(BASE, PaymentPeriodType.WEEKLY, 1))
                    .isEqualTo(BASE.plusWeeks(1));
        }

        @Test
        @DisplayName("BI_WEEKLY → adds 2 weeks")
        void biWeekly_addsTwoWeeks() {
            assertThat(PaymentPeriodUtil.advanceDate(BASE, PaymentPeriodType.BI_WEEKLY, 1))
                    .isEqualTo(BASE.plusWeeks(2));
        }

        @Test
        @DisplayName("MONTHLY → adds 1 month (preserving originalDay)")
        void monthly_addsOneMonth() {
            assertThat(PaymentPeriodUtil.advanceDate(BASE, PaymentPeriodType.MONTHLY, 1))
                    .isEqualTo(BASE.plusMonths(1).withDayOfMonth(1));
        }

        @Test
        @DisplayName("MONTHLY clamping → Jan 31 + 1 month = Feb 28 (non-leap)")
        void monthly_clampsDayToMonthEnd() {
            final LocalDateTime jan31 = LocalDateTime.of(2025, 1, 31, 0, 0);
            final LocalDateTime result = PaymentPeriodUtil.advanceDate(jan31, PaymentPeriodType.MONTHLY, 31);
            assertThat(result.getDayOfMonth()).isEqualTo(28);
        }

        @Test
        @DisplayName("QUARTERLY → adds 3 months")
        void quarterly_addsThreeMonths() {
            assertThat(PaymentPeriodUtil.advanceDate(BASE, PaymentPeriodType.QUARTERLY, 1))
                    .isEqualTo(BASE.plusMonths(3).withDayOfMonth(1));
        }

        @Test
        @DisplayName("HALF_YEARLY → adds 6 months")
        void halfYearly_addsSixMonths() {
            assertThat(PaymentPeriodUtil.advanceDate(BASE, PaymentPeriodType.HALF_YEARLY, 1))
                    .isEqualTo(BASE.plusMonths(6).withDayOfMonth(1));
        }

        @Test
        @DisplayName("YEARLY → adds 1 year")
        void yearly_addsOneYear() {
            assertThat(PaymentPeriodUtil.advanceDate(BASE, PaymentPeriodType.YEARLY, 1))
                    .isEqualTo(BASE.plusYears(1).withDayOfMonth(1));
        }
    }

    // ─── getPeriodEnd ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPeriodEnd")
    class GetPeriodEnd {

        @Test
        @DisplayName("MONTHLY period end = periodStart + 1 month")
        void monthly_periodEnd_isPlusOneMonth() {
            final LocalDateTime periodStart = LocalDateTime.of(2025, 3, 1, 0, 0);
            final LocalDateTime end = PaymentPeriodUtil.getPeriodEnd(periodStart, PaymentPeriodType.MONTHLY, 1);
            assertThat(end).isEqualTo(periodStart.plusMonths(1).withDayOfMonth(1));
        }

        @Test
        @DisplayName("ONE_TIME period end = null")
        void oneTime_periodEnd_isNull() {
            assertThat(PaymentPeriodUtil.getPeriodEnd(BASE, PaymentPeriodType.ONE_TIME, 1)).isNull();
        }

        @Test
        @DisplayName("WEEKLY period end = periodStart + 1 week")
        void weekly_periodEnd_isPlusOneWeek() {
            assertThat(PaymentPeriodUtil.getPeriodEnd(BASE, PaymentPeriodType.WEEKLY, 1))
                    .isEqualTo(BASE.plusWeeks(1));
        }
    }

    // ─── getNextDueDate ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getNextDueDate")
    class GetNextDueDate {

        @Test
        @DisplayName("null firstPaymentDate → returns null")
        void nullFirstPaymentDate_returnsNull() {
            assertThat(PaymentPeriodUtil.getNextDueDate(null, PaymentPeriodType.MONTHLY, BASE)).isNull();
        }

        @Test
        @DisplayName("ONE_TIME → returns firstPaymentDate itself")
        void oneTime_returnsFirstPaymentDate() {
            final LocalDateTime now = BASE.plusDays(10);
            assertThat(PaymentPeriodUtil.getNextDueDate(BASE, PaymentPeriodType.ONE_TIME, now)).isEqualTo(BASE);
        }

        @Test
        @DisplayName("MONTHLY, mid-period → returns end of the current period")
        void monthly_midPeriod_returnsCurrentPeriodEnd() {
            final LocalDateTime firstPayment = LocalDateTime.of(2025, 1, 15, 0, 0);
            final LocalDateTime now = LocalDateTime.of(2025, 3, 20, 0, 0);
            final LocalDateTime due = PaymentPeriodUtil.getNextDueDate(firstPayment, PaymentPeriodType.MONTHLY, now);
            assertThat(due).isEqualTo(LocalDateTime.of(2025, 4, 15, 0, 0));
        }
    }
}
