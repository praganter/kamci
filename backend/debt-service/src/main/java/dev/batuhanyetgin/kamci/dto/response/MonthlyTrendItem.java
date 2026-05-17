package dev.batuhanyetgin.kamci.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class MonthlyTrendItem {

    private String month;

    private BigDecimal totalPaid;

    private int paidCount;

    private int skippedCount;

    private int pendingCount;

    private BigDecimal debtToIncomeRatio;
}
