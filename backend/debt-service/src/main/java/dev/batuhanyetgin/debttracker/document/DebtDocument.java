package dev.batuhanyetgin.debttracker.document;

import dev.batuhanyetgin.commonmodule.document.BaseDocument;
import dev.batuhanyetgin.debttracker.document.embedded.PaymentEmbedded;
import dev.batuhanyetgin.debttracker.document.embedded.ReminderEmbedded;
import dev.batuhanyetgin.debttracker.enums.CurrencyType;
import dev.batuhanyetgin.debttracker.enums.DebtStatus;
import dev.batuhanyetgin.debttracker.enums.PaymentPeriodType;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
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
    private String name;

    @Indexed
    @Field("owner_id")
    private String ownerId;

    @Indexed
    @Field("category_id")
    private String categoryId;

    @Field("description")
    private String description;

    @Field("first_payment_date")
    private LocalDateTime firstPaymentDate;

    @Field("last_payment_date")
    private LocalDateTime lastPaymentDate;

    @Field("payment_period_type")
    private PaymentPeriodType paymentPeriodType;

    @Field("amount")
    private BigDecimal amount;

    @Field("currency")
    private CurrencyType currency;

    @Builder.Default
    @Field("status")
    private DebtStatus status = DebtStatus.ACTIVE;

    @Builder.Default
    @Field("auto_payment")
    private boolean autoPayment = false;

    @Builder.Default
    @Field("payments")
    private List<PaymentEmbedded> payments = List.of();

    @Field("reminder")
    private ReminderEmbedded reminder;

    @Indexed
    @Field("next_reminder_date")
    private LocalDateTime nextReminderDate;

    @Indexed
    @Field("next_auto_payment_date")
    private LocalDateTime nextAutoPaymentDate;
}
