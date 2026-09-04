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

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final RateLimitProperties properties;
    private final ErrorResponseWriter errorResponseWriter;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        List<RateLimitProperties.Rule> rules = properties.getRules();

        if (isBypassed(exchange)) {
            return chain.filter(stripBypassHeader(exchange));
        }

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

    /**
     * True when the request carries the configured bypass header with a value
     * that matches {@code rate-limit.bypassSecret}. Disabled (always false) when
     * the secret is blank. Comparison is constant-time to avoid leaking the
     * secret via timing.
     */
    private boolean isBypassed(ServerWebExchange exchange) {
        String secret = properties.getBypassSecret();
        if (secret == null || secret.isBlank()) {
            return false;
        }
        String presented = exchange.getRequest().getHeaders().getFirst(properties.getBypassHeader());
        if (presented == null) {
            return false;
        }
        return MessageDigest.isEqual(
                presented.getBytes(StandardCharsets.UTF_8),
                secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Removes the bypass header so it is never forwarded to downstream services. */
    private ServerWebExchange stripBypassHeader(ServerWebExchange exchange) {
        return exchange.mutate()
                .request(r -> r.headers(h -> h.remove(properties.getBypassHeader())))
                .build();
    }

    private Bucket buildBucket(RateLimitProperties.Rule rule) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(rule.getCapacity())
                .refillGreedy(rule.getRefillTokens(), Duration.ofSeconds(rule.getRefillSeconds()))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Resolves the client IP from the raw Netty TCP connection, NOT from any
     * X-Forwarded-For / Forwarded header. There is no trusted reverse proxy in
     * front of the gateway (NodePort is exposed directly), so forwarded headers
     * are attacker-controlled and are stripped by {@link ForwardedHeaderStrippingFilter}.
     * If a reverse proxy / ingress is ever placed in front of the gateway, this
     * logic must be revisited to trust the proxy-supplied XFF instead.
     */
    private String resolveIp(ServerWebExchange exchange) {
        InetSocketAddress address = exchange.getRequest().getRemoteAddress();
        return address != null && address.getAddress() != null
                ? address.getAddress().getHostAddress()
                : "unknown";
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
