package dev.batuhanyetgin.authservice.document;

import dev.batuhanyetgin.authservice.enums.HouseholdStatus;
import dev.batuhanyetgin.commonmodule.document.BaseDocument;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Document(collection = "household")
public class HouseholdDocument extends BaseDocument {

    @Indexed
    @Field("owner_id")
    private String ownerId;

    @Field("owner_email")
    private String ownerEmail;

    @Indexed
    @Field("partner_id")
    private String partnerId;

    @Field("partner_email")
    private String partnerEmail;

    @Field("status")
    private HouseholdStatus status;
}
