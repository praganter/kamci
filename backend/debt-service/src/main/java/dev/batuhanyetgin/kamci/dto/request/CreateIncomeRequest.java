package dev.batuhanyetgin.kamci.dto.request;

import dev.batuhanyetgin.kamci.enums.CurrencyType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CreateIncomeRequest {

    @NotNull(message = "{error.validation.not_null}")
    @Min(value = 0, message = "{error.validation.min}")
    private BigDecimal amount;

    @NotNull(message = "{error.validation.not_null}")
    private CurrencyType currency;

    @NotNull(message = "{error.validation.not_null}")
    private LocalDateTime effectiveFrom;

    @Size(max = 200, message = "{error.validation.size}")
    private String note;
}
