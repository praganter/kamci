package dev.batuhanyetgin.commonmodule.document;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public abstract class BaseDocument extends BaseMongoDocument {

    @CreatedDate
    @Field("created_date")
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Field("updated_date")
    private LocalDateTime updatedDate;
}