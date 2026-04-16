package dev.batuhanyetgin.kamci.dto.response;

import dev.batuhanyetgin.kamci.enums.CurrencyType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class IncomeResponse {

    private String id;

    private BigDecimal amount;

    private CurrencyType currency;

    private LocalDateTime effectiveFrom;

    private String note;

    private LocalDateTime createdDate;
}
