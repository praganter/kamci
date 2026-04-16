package dev.batuhanyetgin.kamci.dto.response;

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

    private BigDecimal amountPaid;

    private boolean paid;

    private boolean skipped;

    private LocalDateTime paidTime;
}
