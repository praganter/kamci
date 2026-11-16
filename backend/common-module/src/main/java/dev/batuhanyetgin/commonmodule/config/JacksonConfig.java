package dev.batuhanyetgin.commonmodule.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;

@Configuration
public class JacksonConfig {

    /**
     * Jackson 3 only auto-detects public creators. Request DTOs use Lombok {@code @Builder},
     * whose all-args constructor is package-private, so without this every request body
     * would fail to deserialize (500).
     */
    @Bean
    public JsonMapperBuilderCustomizer anyCreatorVisibility() {
        // Jackson 3 also rejects a missing/null primitive by default; Jackson 2 (the API contract
        // the mobile client was built against) defaulted such fields to false/0.
        return builder -> builder
                .changeDefaultVisibility(vc -> vc.withVisibility(PropertyAccessor.CREATOR, Visibility.ANY))
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
    }
}
