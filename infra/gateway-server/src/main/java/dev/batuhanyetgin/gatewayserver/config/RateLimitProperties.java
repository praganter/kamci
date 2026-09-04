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

    /**
     * Shared secret that, when presented in {@link #bypassHeader}, lets a request
     * skip rate limiting entirely (used by E2E test suites). When blank, the
     * bypass is disabled and the header is ignored — this is the safe default for
     * environments where {@code RATE_LIMIT_BYPASS_SECRET} is not set.
     */
    private String bypassSecret = "";

    /** Header carrying the bypass secret. */
    private String bypassHeader = "X-RateLimit-Bypass";

    @Getter
    @Setter
    public static class Rule {
        private String path;
        private int capacity;
        private int refillTokens;
        private long refillSeconds;
    }
}