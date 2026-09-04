package dev.batuhanyetgin.gatewayserver.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    private List<Rule> rules = List.of();

    @Getter
    @Setter
    public static class Rule {
        private String path;
        private int capacity;
        private int refillTokens;
        private long refillSeconds;
    }
}