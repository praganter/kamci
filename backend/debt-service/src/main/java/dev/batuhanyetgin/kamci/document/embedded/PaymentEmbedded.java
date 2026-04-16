package dev.batuhanyetgin.kamci.document.embedded;

import dev.batuhanyetgin.commonmodule.document.BaseMongoDocument;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEmbedded extends BaseMongoDocument {

    private boolean paid;

    private LocalDateTime paidTime;

    private LocalDateTime periodStart;

    private LocalDateTime periodEnd;

    private BigDecimal amountPaid;
}