package dev.batuhanyetgin.debttracker.dto.response;

import dev.batuhanyetgin.debttracker.document.embedded.PaymentEmbedded;
import dev.batuhanyetgin.debttracker.document.embedded.ReminderEmbedded;
import dev.batuhanyetgin.debttracker.enums.DebtStatus;
import dev.batuhanyetgin.debttracker.enums.PaymentPeriodType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class DebtItemResponse {

    private String id;

    private String name;

    private String ownerId;

    private String categoryId;

    private String description;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private PaymentPeriodType paymentPeriodType;

    private BigDecimal amount;

    private DebtStatus status;

    private List<PaymentEmbedded> payments;

    private ReminderEmbedded reminder;

    private LocalDateTime nextReminderDate;

    private LocalDateTime createdDate;

    private LocalDateTime updatedDate;
}