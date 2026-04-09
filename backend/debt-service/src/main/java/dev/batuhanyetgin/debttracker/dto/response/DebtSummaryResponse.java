package dev.batuhanyetgin.debttracker.dto.response;

import dev.batuhanyetgin.debttracker.enums.CurrencyType;
import dev.batuhanyetgin.debttracker.enums.DebtStatus;
import dev.batuhanyetgin.debttracker.enums.PaymentPeriodType;
import dev.batuhanyetgin.debttracker.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class DebtSummaryResponse {

    private String id;

    private String name;

    private String categoryId;

    private Integer installmentCount;

    private int paidCount;

    private PaymentPeriodType paymentPeriodType;

    private BigDecimal amount;

    private CurrencyType currency;

    private DebtStatus status;

    private PaymentStatus paymentStatus;

    private boolean autoPayment;

    private boolean paidCurrentPeriod;

    private boolean hasReminder;
}
