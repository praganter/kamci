package dev.batuhanyetgin.authservice.document;

import dev.batuhanyetgin.commonmodule.document.BaseDocument;
import jakarta.validation.constraints.NotNull;
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
@Document(collection = "auth")
public class AuthDocument extends BaseDocument {

    @Indexed(unique = true)
    @Field("email")
    private String email;

    @NotNull
    @Field("password")
    private String password;

    @Builder.Default
    @Field("premium")
    private boolean premium = false;
}