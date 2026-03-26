package dev.batuhanyetgin.debttracker.dto.response;

import dev.batuhanyetgin.debttracker.document.embedded.ReminderEmbedded;
import dev.batuhanyetgin.debttracker.enums.DebtStatus;
import dev.batuhanyetgin.debttracker.enums.PaymentPeriodType;
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

    private LocalDateTime lastPaymentDate;

    private PaymentPeriodType paymentPeriodType;

    private BigDecimal amount;

    private DebtStatus status;

    private boolean autoPayment;

    private ReminderEmbedded reminder;

    private LocalDateTime nextReminderDate;

    private LocalDateTime nextAutoPaymentDate;

    private LocalDateTime createdDate;

    private LocalDateTime updatedDate;

    private boolean paidCurrentPeriod;
}
