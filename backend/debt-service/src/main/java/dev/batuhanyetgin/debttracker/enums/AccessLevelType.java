package dev.batuhanyetgin.debttracker.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ShareType {
    READ('R'),
    EDIT('E'),
    FULL('F');

    private final Character value;
}