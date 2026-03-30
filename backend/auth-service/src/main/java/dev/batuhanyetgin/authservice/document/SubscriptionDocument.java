package dev.batuhanyetgin.authservice.document;

import dev.batuhanyetgin.authservice.enums.SubscriptionPlan;
import dev.batuhanyetgin.authservice.enums.SubscriptionPlatform;
import dev.batuhanyetgin.authservice.enums.SubscriptionStatus;
import dev.batuhanyetgin.commonmodule.document.BaseDocument;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Document(collection = "subscriptions")
public class SubscriptionDocument extends BaseDocument {

    @Indexed(unique = true)
    @Field("user_id")
    private String userId;

    @Field("plan")
    private SubscriptionPlan plan;

    @Field("platform")
    private SubscriptionPlatform platform;

    @Field("status")
    private SubscriptionStatus status;

    @Indexed
    @Field("original_transaction_id")
    private String originalTransactionId;

    @Indexed
    @Field("purchase_token")
    private String purchaseToken;

    @Field("product_id")
    private String productId;

    @Field("start_date")
    private LocalDateTime startDate;

    @Field("expiry_date")
    private LocalDateTime expiryDate;
}
