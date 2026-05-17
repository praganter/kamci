package dev.batuhanyetgin.kamci.dto.response;

import dev.batuhanyetgin.kamci.enums.CurrencyType;
import dev.batuhanyetgin.kamci.enums.DebtStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class DebtProgressItem {

    private String debtId;

    private String debtName;

    private CurrencyType currency;

    private BigDecimal totalAmount;

    private BigDecimal totalPaid;

    private BigDecimal remainingAmount;

    private int paidInstallments;

    private Integer totalInstallments;

    private BigDecimal completionPercentage;

    private DebtStatus status;
}
