package dev.batuhanyetgin.debttracker.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CurrencyResponse {
    private String code;
    private String symbol;
}
