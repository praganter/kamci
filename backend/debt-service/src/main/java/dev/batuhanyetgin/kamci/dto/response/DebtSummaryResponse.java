package dev.batuhanyetgin.kamci.dto.response;

import dev.batuhanyetgin.kamci.enums.CurrencyType;
import dev.batuhanyetgin.kamci.enums.DebtStatus;
import dev.batuhanyetgin.kamci.enums.PaymentPeriodType;
import dev.batuhanyetgin.kamci.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class DebtSummaryResponse {

    private String id;

    private String name;

    private String categoryId;

    private LocalDateTime nextDueDate;

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
