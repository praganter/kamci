package dev.batuhanyetgin.notificationservice.document;

import dev.batuhanyetgin.commonmodule.document.BaseDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "device_tokens")
public class DeviceTokenDocument extends BaseDocument {

    @Indexed(unique = true)
    @Field("user_id")
    private String userId;

    @Field("fcm_token")
    private String fcmToken;

    @Field("locale")
    private String locale;
}
