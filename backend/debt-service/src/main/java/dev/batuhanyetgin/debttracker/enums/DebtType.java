package dev.batuhanyetgin.debttracker.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DebtType {
    CREDIT('C'),
    SUBSCRIPTION('S'),
    TAKEN('T'),
    GIVEN('G');

    public final Character value;
}