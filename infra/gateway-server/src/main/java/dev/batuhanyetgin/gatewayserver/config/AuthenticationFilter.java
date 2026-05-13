package dev.batuhanyetgin.gatewayserver.config;

import dev.batuhanyetgin.gatewayserver.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final RouterValidator validator;
    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (validator.isSecured().test(request)) {
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return this.onError(exchange, "Authorization header is missing", HttpStatus.UNAUTHORIZED);
            }

            final String authHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            String token = jwtUtil.getAuthHeader(authHeader);

            if (token == null) {
                return this.onError(exchange, "Authorization header is invalid", HttpStatus.UNAUTHORIZED);
            }

            Claims claims;
            try {
                claims = jwtUtil.getAllClaimsFromToken(token);
            } catch (Exception e) {
                return this.onError(exchange, "Authorization header is invalid", HttpStatus.UNAUTHORIZED);
            }

            if (jwtUtil.isInvalid(claims)) {
                return this.onError(exchange, "JWT is invalid or expired", HttpStatus.FORBIDDEN);
            }

            final String userId = claims.getSubject();
            final boolean isPremium = Boolean.TRUE.equals(claims.get("premium", Boolean.class));
            exchange = exchange.mutate()
                    .request(r -> r.headers(headers -> {
                        headers.remove("user-id");
                        headers.remove("User-ID");
                        headers.add("user-id", userId);
                        headers.remove("user-premium");
                        headers.add("user-premium", String.valueOf(isPremium));
                    }))
                    .build();
        }

        return chain.filter(exchange);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        String errorResponse = String.format("{\"error\": \"%s\", \"status\": %d}", err, httpStatus.value());
        byte[] bytes = errorResponse.getBytes(StandardCharsets.UTF_8);

        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}