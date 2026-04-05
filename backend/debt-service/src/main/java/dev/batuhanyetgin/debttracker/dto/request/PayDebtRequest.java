package dev.batuhanyetgin.debttracker.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayDebtRequest {

    @Min(value = 0, message = "{error.validation.min}")
    private BigDecimal amountPaid;
}
