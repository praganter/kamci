package dev.batuhanyetgin.debttracker.document.embedded;

import dev.batuhanyetgin.commonmodule.document.BaseMongoDocument;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEmbedded extends BaseMongoDocument {

    private boolean paid;

    private LocalDateTime paidTime;
}