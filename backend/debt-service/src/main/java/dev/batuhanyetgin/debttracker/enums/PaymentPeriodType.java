package dev.batuhanyetgin.debttracker.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentPeriodType {
    DAILY('D'),
    WEEKLY('W'),
    BI_WEEKLY('B'),
    MONTHLY('M'),
    QUARTERLY('Q'),
    HALF_YEARLY('H'),
    YEARLY('Y');

    private final Character value;
}