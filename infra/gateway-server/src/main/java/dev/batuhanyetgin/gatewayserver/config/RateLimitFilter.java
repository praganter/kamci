package dev.batuhanyetgin.gatewayserver.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final RateLimitProperties properties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        List<RateLimitProperties.Rule> rules = properties.getRules();

        for (RateLimitProperties.Rule rule : rules) {
            if (path.equals(rule.getPath())) {
                String ip = resolveIp(exchange);
                String key = ip + ":" + rule.getPath();
                Bucket bucket = buckets.computeIfAbsent(key, k -> buildBucket(rule));

                if (bucket.tryConsume(1)) {
                    return chain.filter(exchange);
                } else {
                    return onRateLimitExceeded(exchange);
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

    private String resolveIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(InetSocketAddress::getHostString)
                .orElse("unknown");
    }

    private Mono<Void> onRateLimitExceeded(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"error\": \"Too many requests, please try again later.\", \"status\": 429}";
        byte[] bytes = body.getBytes();
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
