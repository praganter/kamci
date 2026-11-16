package dev.batuhanyetgin.gatewayserver.config;

import tools.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Writes gateway-originated error responses (auth failures, rate limiting) in
 * the shared {@code {timestamp,status,code,message,path,traceId,errors}}
 * envelope also used by {@code common-module}'s {@code ErrorResponse}, so
 * clients (mobile) can parse gateway and downstream service errors the same
 * way. See dotted `code` contract: error.auth.*, error.ratelimit.*.
 */
@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

    private final ObjectMapper objectMapper;
    private final ObjectProvider<Tracer> tracerProvider;

    public Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        GatewayErrorResponse body = new GatewayErrorResponse(
                Instant.now(),
                status.value(),
                code,
                message,
                exchange.getRequest().getURI().getPath(),
                resolveTraceId(),
                List.of()
        );

        return response.writeWith(Mono.just(response.bufferFactory().wrap(serialize(body))));
    }

    private String resolveTraceId() {
        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer == null || tracer.currentSpan() == null) {
            return null;
        }
        return tracer.currentSpan().context().traceId();
    }

    private byte[] serialize(GatewayErrorResponse body) {
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            String fallback = String.format(
                    "{\"code\":\"error.internal\",\"status\":%d,\"message\":\"Internal error\"}",
                    body.status());
            return fallback.getBytes(StandardCharsets.UTF_8);
        }
    }
}
