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
import org.springframework.data.mongodb.core.mapping.Field;

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

    @Field("name")
    @NotNull
    private String name;

    @Field("owner_id")
    private String ownerId;

    @Field("description")
    private String description;

    @Field("start_date")
    @NotNull
    private LocalDateTime startDate;

    @Field("end_date")
    private LocalDateTime endDate;

    @NotNull
    @Field("payment_period_type")
    private PaymentPeriodType paymentPeriodType;

    @NotNull
    @Field("amount")
    private Double amount;

    @NotNull
    @Builder.Default
    @Field("status")
    private DebtStatus status = DebtStatus.ACTIVE;

    @Field("shared_with")
    private List<SharedWithEmbedded> sharedWith;

    @NotNull
    @Field("category")
    private CategoryEmbedded category;

    @Field("payments")
    private List<PaymentEmbedded> payments;
}