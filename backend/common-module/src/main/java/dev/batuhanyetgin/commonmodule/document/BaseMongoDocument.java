package dev.batuhanyetgin.commonmodule.document;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public abstract class BaseMongoDocument {

    @MongoId(FieldType.OBJECT_ID)
    private String id;
}