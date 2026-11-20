package dev.batuhanyetgin.commonmodule.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Adds the internal token to Feign calls that target an {@code internal} path. */
@Configuration
@ConditionalOnClass(name = "feign.RequestInterceptor")
public class InternalFeignConfig {

    @Bean
    public RequestInterceptor internalTokenRequestInterceptor(
            @Value("${INTERNAL_API_TOKEN:" + InternalApiConfig.DEV_DEFAULT_TOKEN + "}") final String token) {
        return template -> {
            if (template.path().contains("/internal/")) {
                template.header(InternalApiConfig.HEADER, token);
            }
        };
    }
}
