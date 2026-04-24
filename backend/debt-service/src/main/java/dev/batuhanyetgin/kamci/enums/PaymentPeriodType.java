package dev.batuhanyetgin.kamci.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentPeriodType {
    ONE_TIME('O'),
    DAILY('D'),
    WEEKLY('W'),
    BI_WEEKLY('B'),
    MONTHLY('M'),
    QUARTERLY('Q'),
    HALF_YEARLY('H'),
    YEARLY('Y');

    private final Character value;
}