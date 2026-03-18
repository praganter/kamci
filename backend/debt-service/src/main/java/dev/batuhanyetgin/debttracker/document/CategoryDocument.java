package dev.batuhanyetgin.debttracker.document;

import dev.batuhanyetgin.commonmodule.document.BaseDocument;
import dev.batuhanyetgin.debttracker.document.embedded.MemberEmbedded;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Document(collection = "category")
public class CategoryDocument extends BaseDocument {

    @Indexed
    @Field("owner_id")
    private String ownerId;

    @Field("name")
    private String name;

    @Indexed
    @Builder.Default
    @Field("members")
    private List<MemberEmbedded> members = new ArrayList<>();
}