package dev.batuhanyetgin.debttracker.util;

import dev.batuhanyetgin.debttracker.enums.PaymentPeriodType;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public final class PaymentPeriodUtil {

    private PaymentPeriodUtil() {}

    public static LocalDateTime getPeriodStart(final LocalDateTime firstPaymentDate,
                                               final PaymentPeriodType periodType,
                                               final LocalDateTime now) {
        if (firstPaymentDate == null || firstPaymentDate.isAfter(now)) {
            return now;
        }
        return switch (periodType) {
            case DAILY -> {
                final long days = ChronoUnit.DAYS.between(firstPaymentDate, now);
                yield firstPaymentDate.plusDays(days);
            }
            case WEEKLY -> {
                final long weeks = ChronoUnit.WEEKS.between(firstPaymentDate, now);
                yield firstPaymentDate.plusWeeks(weeks);
            }
            case BI_WEEKLY -> {
                final long biWeeks = ChronoUnit.WEEKS.between(firstPaymentDate, now) / 2;
                yield firstPaymentDate.plusWeeks(biWeeks * 2);
            }
            case MONTHLY -> {
                final long months = ChronoUnit.MONTHS.between(firstPaymentDate, now);
                yield firstPaymentDate.plusMonths(months);
            }
            case QUARTERLY -> {
                final long quarters = ChronoUnit.MONTHS.between(firstPaymentDate, now) / 3;
                yield firstPaymentDate.plusMonths(quarters * 3);
            }
            case HALF_YEARLY -> {
                final long halfYears = ChronoUnit.MONTHS.between(firstPaymentDate, now) / 6;
                yield firstPaymentDate.plusMonths(halfYears * 6);
            }
            case YEARLY -> {
                final long years = ChronoUnit.YEARS.between(firstPaymentDate, now);
                yield firstPaymentDate.plusYears(years);
            }
        };
    }

    public static LocalDateTime getPeriodEnd(final LocalDateTime periodStart,
                                             final PaymentPeriodType type,
                                             final int originalDay) {
        return advanceDate(periodStart, type, originalDay);
    }

    public static LocalDateTime advanceDate(final LocalDateTime current,
                                            final PaymentPeriodType type,
                                            final int originalDay) {
        return switch (type) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case BI_WEEKLY -> current.plusWeeks(2);
            case MONTHLY -> clampToDay(current.plusMonths(1), originalDay);
            case QUARTERLY -> clampToDay(current.plusMonths(3), originalDay);
            case HALF_YEARLY -> clampToDay(current.plusMonths(6), originalDay);
            case YEARLY -> clampToDay(current.plusYears(1), originalDay);
        };
    }

    private static LocalDateTime clampToDay(final LocalDateTime dt, final int targetDay) {
        final int maxDay = dt.toLocalDate().lengthOfMonth();
        return dt.withDayOfMonth(Math.min(targetDay, maxDay));
    }
}
