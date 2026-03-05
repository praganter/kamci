package dev.batuhanyetgin.debttracker.document;

import dev.batuhanyetgin.commonmodule.document.BaseDocument;
import dev.batuhanyetgin.debttracker.document.embedded.CategoryEmbedded;
import dev.batuhanyetgin.debttracker.document.embedded.PaymentEmbedded;
import dev.batuhanyetgin.debttracker.document.embedded.SharedWithEmbedded;
import dev.batuhanyetgin.debttracker.enums.DebtStatus;
import dev.batuhanyetgin.debttracker.enums.PaymentPeriodType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Document(collection = "debt")
public class DebtDocument extends BaseDocument {

    @NotNull
    private String name;

    private String ownerId;

    private String description;

    @NotNull
    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @NotNull
    private PaymentPeriodType paymentPeriodType;

    @NotNull
    private Double amount;

    @NotNull
    @Builder.Default
    private DebtStatus status = DebtStatus.ACTIVE;

    private List<SharedWithEmbedded> sharedWith;

    @NotNull
    private CategoryEmbedded category;

    private List<PaymentEmbedded> payments;
}