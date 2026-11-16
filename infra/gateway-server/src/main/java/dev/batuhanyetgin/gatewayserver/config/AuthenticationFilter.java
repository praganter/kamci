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
import org.springframework.http.server.PathContainer;
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

        // Inter-service endpoints (e.g. debt/internal/**) must never be reachable from outside.
        // Checks decoded segments so %-encoding / matrix params / discovery-locator prefixes can't bypass it.
        boolean isInternal = request.getPath().pathWithinApplication().elements().stream()
                .anyMatch(e -> e instanceof PathContainer.PathSegment s && "internal".equalsIgnoreCase(s.valueToMatch()));
        if (isInternal) {
            return errorResponseWriter.write(exchange, HttpStatus.NOT_FOUND,
                    "error.route.not_found", "Not found");
        }

        // Identity headers are gateway-owned: drop any client-supplied value on every route (public ones included).
        exchange = exchange.mutate()
                .request(r -> r.headers(h -> {
                    h.remove("user-id");
                    h.remove("user-premium");
                }))
                .build();

        if (validator.isSecured().test(request)) {
            final String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null) {
                return errorResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED,
                        "error.auth.header_missing", "Authorization header is missing");
            }

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
                        headers.set("user-id", userId);
                        headers.set("user-premium", String.valueOf(isPremium));
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
