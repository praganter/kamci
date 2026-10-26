package dev.batuhanyetgin.gatewayserver.config;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Strips client-supplied forwarding headers from every inbound request before
 * any other filter runs.
 *
 * <p>The gateway is exposed directly (NodePort, no reverse proxy / ingress in
 * front), so an external client can freely spoof {@code X-Forwarded-For} /
 * {@code Forwarded} / {@code X-Real-IP} to impersonate arbitrary source IPs.
 * That would let a client bypass {@link RateLimitFilter} (which keys buckets by
 * client IP) simply by rotating a fake header. We drop these headers here; the
 * gateway's own forwarded-headers filter then re-adds trusted values based on
 * the real Netty TCP {@code remoteAddress} for downstream services.
 *
 * <p>Requests whose TCP peer is a configured {@link TrustedProxies trusted proxy}
 * (the TLS-terminating ingress) keep their headers — there the proxy-supplied
 * XFF is legitimate.
 */
@Component
@RequiredArgsConstructor
public class ForwardedHeaderStrippingFilter implements GlobalFilter, Ordered {

    private final TrustedProxies trustedProxies;

    private static final List<String> STRIPPED_HEADERS = List.of(
            "X-Forwarded-For",
            "X-Forwarded-Host",
            "X-Forwarded-Proto",
            "X-Forwarded-Port",
            "X-Forwarded-Prefix",
            "Forwarded",
            "X-Real-IP"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (trustedProxies.isTrusted(exchange)) {
            return chain.filter(exchange);
        }
        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.headers(headers -> STRIPPED_HEADERS.forEach(headers::remove)))
                .build();
        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
