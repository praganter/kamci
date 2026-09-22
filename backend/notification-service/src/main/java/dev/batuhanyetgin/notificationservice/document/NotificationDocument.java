package dev.batuhanyetgin.notificationservice.document;

import dev.batuhanyetgin.commonmodule.document.BaseDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Document(collection = "notifications")
public class NotificationDocument extends BaseDocument {

    @Indexed
    @Field("user_id")
    private String userId;

    @Field("type")
    private NotificationType type;

    @Field("title")
    private String title;

    @Field("body")
    private String body;

    @Field("entity_id")
    private String entityId;

    @Builder.Default
    @Field("read")
    private boolean read = false;
}
