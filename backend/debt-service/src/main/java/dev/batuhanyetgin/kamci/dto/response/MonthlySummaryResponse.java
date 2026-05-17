package dev.batuhanyetgin.kamci.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class MonthlySummaryResponse {

    private String month;

    private BigDecimal totalPaid;

    private int paidCount;

    private int skippedCount;

    private BigDecimal skippedAmount;

    private int pendingCount;

    private BigDecimal pendingAmount;

    private BigDecimal incomeAtMonth;

    private BigDecimal debtToIncomeRatio;

    private List<DebtMonthlySummaryItem> debts;

    private List<MonthlyTrendItem> trend;

    private List<CategoryBreakdownItem> categoryBreakdown;

    private List<DebtProgressItem> debtProgress;
}
