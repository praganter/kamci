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
public class DebtMonthlySummaryItem {

    private String debtId;

    private String debtName;

    private CurrencyType currency;

    private BigDecimal amountPaid;

    private boolean paid;

    private boolean skipped;

    private boolean pending;

    private LocalDateTime paidTime;
}
