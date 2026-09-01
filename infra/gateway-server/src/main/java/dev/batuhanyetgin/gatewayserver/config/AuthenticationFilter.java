package dev.batuhanyetgin.gatewayserver.config;

import dev.batuhanyetgin.gatewayserver.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final RouterValidator validator;
    private final JwtUtil jwtUtil;
    private final ErrorResponseWriter errorResponseWriter;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (validator.isSecured().test(request)) {
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return errorResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED,
                        "error.auth.header_missing", "Authorization header is missing");
            }

            final String authHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            String token = jwtUtil.getAuthHeader(authHeader);

            if (token == null) {
                return errorResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED,
                        "error.auth.header_invalid", "Authorization header is malformed");
            }

            Claims claims;
            try {
                claims = jwtUtil.getAllClaimsFromToken(token);
            } catch (ExpiredJwtException e) {
                return errorResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED,
                        "error.auth.token_expired", "Token has expired");
            } catch (Exception e) {
                return errorResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED,
                        "error.auth.token_invalid", "Token is invalid");
            }

            if (jwtUtil.isWrongType(claims)) {
                return errorResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED,
                        "error.auth.token_invalid", "Token is invalid");
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

    @Override
    public int getOrder() {
        return -1;
    }
}
