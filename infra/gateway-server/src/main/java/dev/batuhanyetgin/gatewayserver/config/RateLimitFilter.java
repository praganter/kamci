package dev.batuhanyetgin.gatewayserver.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final RateLimitProperties properties;
    private final ErrorResponseWriter errorResponseWriter;
    private final TrustedProxies trustedProxies;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        List<RateLimitProperties.Rule> rules = properties.getRules();

        for (RateLimitProperties.Rule rule : rules) {
            if (pathMatcher.match(rule.getPath(), path)) {
                String ip = resolveIp(exchange);
                String key = ip + ":" + rule.getPath();
                Bucket bucket = buckets.get(key, k -> buildBucket(rule));

                if (bucket.tryConsume(1)) {
                    return chain.filter(exchange);
                } else {
                    return errorResponseWriter.write(exchange, HttpStatus.TOO_MANY_REQUESTS,
                            "error.ratelimit.exceeded", "Too many requests, please try again later");
                }
            }
        }

        return chain.filter(exchange);
    }

    private Bucket buildBucket(RateLimitProperties.Rule rule) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(rule.getCapacity())
                .refillGreedy(rule.getRefillTokens(), Duration.ofSeconds(rule.getRefillSeconds()))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Resolves the client IP from the raw TCP connection, or from the proxy-set
     * X-Forwarded-For hop when the peer is a {@link TrustedProxies trusted proxy}.
     * Untrusted forwarded headers are stripped by {@link ForwardedHeaderStrippingFilter}.
     */
    private String resolveIp(ServerWebExchange exchange) {
        return trustedProxies.clientIp(exchange);
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
