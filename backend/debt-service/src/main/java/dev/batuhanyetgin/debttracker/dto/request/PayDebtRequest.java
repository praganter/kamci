package dev.batuhanyetgin.debttracker.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayDebtRequest {

    @Min(value = 0, message = "{error.validation.min}")
    private BigDecimal amountPaid;

    @PastOrPresent(message = "{error.validation.past_or_present}")
    private LocalDateTime paidTime;
}