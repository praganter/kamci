package dev.batuhanyetgin.debttracker.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DebtStatus {
    PAID('P'),
    UNPAID('U'),
    OVERDUE('O'),
    PARTIALLY_PAID('M');

    private final Character value;
}