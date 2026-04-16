package dev.batuhanyetgin.kamci.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DebtStatus {
    ACTIVE('A'),
    COMPLETED('C');

    private final Character value;
}