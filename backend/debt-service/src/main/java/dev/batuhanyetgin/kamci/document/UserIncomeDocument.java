package dev.batuhanyetgin.kamci.document;

import dev.batuhanyetgin.commonmodule.document.BaseDocument;
import dev.batuhanyetgin.kamci.enums.CurrencyType;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Document(collection = "user_income")
@CompoundIndex(name = "owner_income_idx", def = "{'owner_id': 1, 'effective_from': -1}")
public class UserIncomeDocument extends BaseDocument {

    @Indexed
    @Field("owner_id")
    private String ownerId;

    @Field("amount")
    private BigDecimal amount;

    @Field("currency")
    private CurrencyType currency;

    @Field("effective_from")
    private LocalDateTime effectiveFrom;

    @Field("note")
    private String note;
}
