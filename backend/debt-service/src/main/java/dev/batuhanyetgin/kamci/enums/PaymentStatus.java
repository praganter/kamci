package dev.batuhanyetgin.kamci.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentStatus {
    PAID('P'),
    UNPAID('U'),
    OVERDUE('O'),
    PARTIALLY_PAID('M');

    private final Character value;
}
