package dev.batuhanyetgin.debttracker.dto.request;

import dev.batuhanyetgin.debttracker.document.embedded.CategoryEmbedded;
import dev.batuhanyetgin.debttracker.document.embedded.SharedWithEmbedded;
import dev.batuhanyetgin.debttracker.enums.PaymentPeriodType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class CreateDebtRequest {

    @NotNull
    private String name;

    private String description;

    @NotNull
    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @NotNull
    private PaymentPeriodType paymentPeriodType;

    @NotNull
    private Double amount;

    private List<SharedWithEmbedded> sharedWith;

    @NotNull
    private CategoryEmbedded category;
}