package dev.batuhanyetgin.debttracker.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ShareType {
    READ_ONLY,
    PAY,
    FULL
}