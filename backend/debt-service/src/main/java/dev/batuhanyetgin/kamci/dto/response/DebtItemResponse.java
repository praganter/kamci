package dev.batuhanyetgin.kamci.dto.response;

import dev.batuhanyetgin.kamci.document.embedded.ReminderEmbedded;
import dev.batuhanyetgin.kamci.enums.CurrencyType;
import dev.batuhanyetgin.kamci.enums.DebtStatus;
import dev.batuhanyetgin.kamci.enums.PaymentStatus;
import dev.batuhanyetgin.kamci.enums.PaymentPeriodType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class DebtItemResponse {

    private String id;

    private String name;

    private String ownerId;

    private String categoryId;

    private String description;

    private LocalDateTime firstPaymentDate;

    private Integer installmentCount;

    private PaymentPeriodType paymentPeriodType;

    private BigDecimal amount;

    private CurrencyType currency;

    private DebtStatus status;

    private PaymentStatus paymentStatus;

    private boolean autoPayment;

    private ReminderEmbedded reminder;

    private LocalDateTime nextReminderDate;

    private LocalDateTime nextAutoPaymentDate;

    private LocalDateTime createdDate;

    private LocalDateTime updatedDate;

    private boolean paidCurrentPeriod;
}