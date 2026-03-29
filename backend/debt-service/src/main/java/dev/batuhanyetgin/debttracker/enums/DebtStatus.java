package dev.batuhanyetgin.debttracker.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DebtStatus {
    ACTIVE('A'),
    PASSIVE('P'),
    OVERDUE('O');

    private final Character value;
}