package dev.batuhanyetgin.debttracker.dto.request;

import dev.batuhanyetgin.debttracker.document.embedded.CategoryEmbedded;
import dev.batuhanyetgin.debttracker.document.embedded.SharedWithEmbedded;
import dev.batuhanyetgin.debttracker.enums.PaymentPeriodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class UpdateDebtRequest {

    @NotBlank(message = "{error.validation.not_blank}")
    private String id;

    @NotBlank(message = "{error.validation.not_blank}")
    private String name;

    private String description;

    @NotNull(message = "{error.validation.not_null}")
    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @NotNull(message = "{error.validation.not_null}")
    private PaymentPeriodType paymentPeriodType;

    @NotNull(message = "{error.validation.not_null}")
    private Double amount;

    private List<SharedWithEmbedded> sharedWith;

    @NotNull(message = "{error.validation.not_null}")
    private CategoryEmbedded category;
}