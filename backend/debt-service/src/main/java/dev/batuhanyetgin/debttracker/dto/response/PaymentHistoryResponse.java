package dev.batuhanyetgin.debttracker.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class PaymentHistoryResponse {

    private boolean paid;

    private LocalDateTime paidTime;

    private LocalDateTime periodStart;

    private LocalDateTime periodEnd;

    private BigDecimal amountPaid;
}
