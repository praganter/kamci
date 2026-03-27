package dev.batuhanyetgin.debttracker.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.batuhanyetgin.debttracker.enums.CurrencyType;
import dev.batuhanyetgin.debttracker.enums.PaymentPeriodType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class UpdateDebtRequest {

    @NotBlank(message = "{error.validation.not_blank}")
    @Size(max = 24, message = "{error.validation.size}")
    private String id;

    @NotBlank(message = "{error.validation.not_blank}")
    @Size(max = 150, message = "{error.validation.size}")
    private String name;

    @Size(max = 250, message = "{error.validation.size}")
    private String description;

    @NotNull(message = "{error.validation.not_null}")
    private LocalDateTime firstPaymentDate;

    private LocalDateTime lastPaymentDate;

    @NotNull(message = "{error.validation.not_null}")
    private PaymentPeriodType paymentPeriodType;

    @NotNull(message = "{error.validation.not_null}")
    @Min(value = 0, message = "{error.validation.min}")
    private BigDecimal amount;

    @NotNull(message = "{error.validation.not_null}")
    private CurrencyType currency;

    @NotBlank(message = "{error.validation.not_blank}")
    @Size(max = 24, message = "{error.validation.size}")
    private String categoryId;

    private boolean autoPayment;

    @Valid
    private ReminderRequest reminder;

    @JsonIgnore
    private String ownerId;
}
