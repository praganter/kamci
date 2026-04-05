package dev.batuhanyetgin.debttracker.document;

import dev.batuhanyetgin.commonmodule.document.BaseDocument;
import dev.batuhanyetgin.debttracker.document.embedded.AmountHistoryEmbedded;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @Field("installment_count")
    private Integer installmentCount;

    @Field("payment_period_type")
    private PaymentPeriodType paymentPeriodType;

    @Field("amount")
    private BigDecimal amount;

    @Field("currency")
    private CurrencyType currency;

    @Builder.Default
    @Field("status")
    private DebtStatus status = DebtStatus.UNPAID;

    @Builder.Default
    @Field("auto_payment")
    private boolean autoPayment = false;

    @Builder.Default
    @Field("payments")
    private List<PaymentEmbedded> payments = List.of();

    @Builder.Default
    @Field("amount_history")
    private List<AmountHistoryEmbedded> amountHistory = new ArrayList<>();

    @Field("reminder")
    private ReminderEmbedded reminder;

    @Indexed
    @Field("next_reminder_date")
    private LocalDateTime nextReminderDate;

    @Indexed
    @Field("next_auto_payment_date")
    private LocalDateTime nextAutoPaymentDate;

    public void addPayment(final LocalDateTime paidTime, final LocalDateTime periodStart,
                           final LocalDateTime periodEnd, final BigDecimal currentAmount) {
        final List<PaymentEmbedded> updated = new ArrayList<>(this.payments);
        updated.add(PaymentEmbedded.builder()
                .paid(true)
                .paidTime(paidTime)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .amountPaid(currentAmount)
                .build());
        this.payments = updated;
    }

    public void skipPayment(final LocalDateTime time, final LocalDateTime periodStart,
                            final LocalDateTime periodEnd) {
        final List<PaymentEmbedded> updated = new ArrayList<>(this.payments);
        updated.add(PaymentEmbedded.builder()
                .paid(false)
                .paidTime(time)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .build());
        this.payments = updated;
    }

    public void updateAmount(final BigDecimal newAmount, final LocalDateTime effectiveFrom) {
        if (Objects.equals(this.amount, newAmount)) {
            return;
        }
        final List<AmountHistoryEmbedded> updated = new ArrayList<>(this.amountHistory);
        final int lastIdx = updated.size() - 1;
        if (lastIdx >= 0 && updated.get(lastIdx).getEffectiveTo() == null) {
            updated.get(lastIdx).setEffectiveTo(effectiveFrom);
        }
        updated.add(AmountHistoryEmbedded.builder()
                .amount(this.amount)
                .currency(this.currency)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(null)
                .build());
        this.amountHistory = updated;
        this.amount = newAmount;
    }
}
