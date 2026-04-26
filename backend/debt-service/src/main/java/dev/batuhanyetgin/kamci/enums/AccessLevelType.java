package dev.batuhanyetgin.kamci.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AccessLevelType {
    READ('R'),
    WRITE('W'),
    EDIT('E'),
    DELETE('D');

    private final Character value;
}