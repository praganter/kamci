package dev.batuhanyetgin.debttracker.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentPeriodType {
    DAILY,
    WEEKLY,
    MONTHLY,
    HALF_YEARLY,
    YEARLY
}