package dev.batuhanyetgin.debttracker.document.embedded;

import dev.batuhanyetgin.debttracker.enums.CurrencyType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmountHistoryEmbedded {

    private BigDecimal amount;

    private CurrencyType currency;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;
}
