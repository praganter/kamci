package dev.batuhanyetgin.authservice.dto;

import dev.batuhanyetgin.authservice.dto.request.LoginRequest;
import dev.batuhanyetgin.authservice.dto.request.RegisterRequest;
import dev.batuhanyetgin.commonmodule.config.JacksonConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/** Regression: Boot 4 / Jackson 3 failed on Lombok @Builder request DTOs (package-private constructor). */
class RequestBodyDeserializationTest {

    private final JsonMapper mapper = buildMapper();

    private static JsonMapper buildMapper() {
        final JsonMapper.Builder builder = JsonMapper.builder();
        new JacksonConfig().anyCreatorVisibility().customize(builder);
        return builder.build();
    }

    @Test
    @DisplayName("register body deserializes")
    void register() {
        final RegisterRequest r = mapper.readValue("{\"email\":\"a@b.co\",\"password\":\"Password123\"}", RegisterRequest.class);
        assertThat(r.getEmail()).isEqualTo("a@b.co");
        assertThat(r.getPassword()).isEqualTo("Password123");
    }

    @Test
    @DisplayName("login body deserializes")
    void login() {
        final LoginRequest r = mapper.readValue("{\"email\":\"a@b.co\",\"password\":\"Password123\"}", LoginRequest.class);
        assertThat(r.getEmail()).isEqualTo("a@b.co");
    }

    @Test
    @DisplayName("missing primitive property defaults instead of failing")
    void missingPrimitive() {
        record Body(String name, boolean flag) { }
        assertThat(mapper.readValue("{\"name\":\"x\"}", Body.class).flag()).isFalse();
    }
}
