package dev.batuhanyetgin.debttracker.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.batuhanyetgin.debttracker.document.embedded.CategoryEmbedded;
import dev.batuhanyetgin.debttracker.document.embedded.SharedWithEmbedded;
import dev.batuhanyetgin.debttracker.enums.PaymentPeriodType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class CreateDebtRequest {

    @NotBlank(message = "{error.validation.not_blank}")
    private String name;

    private String description;

    @NotNull(message = "{error.validation.not_null}")
    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @NotNull(message = "{error.validation.not_null}")
    private PaymentPeriodType paymentPeriodType;

    @NotNull(message = "{error.validation.not_null}")
    @Min(value = 0, message = "{error.validation.min}")
    private BigDecimal amount;

    private List<SharedWithEmbedded> sharedWith;

    @Valid
    @NotNull(message = "{error.validation.not_null}")
    private CategoryEmbedded category;

    @JsonIgnore
    private String ownerId;
}