package dev.batuhanyetgin.gatewayserver.config;

import java.time.Instant;
import java.util.List;

/**
 * Mirrors the shape of {@code common-module}'s {@code ErrorResponse} DTO
 * (field-for-field) so mobile/BE consumers can parse gateway and downstream
 * errors with a single model. Not reused directly because common-module pulls
 * in Spring MVC/servlet dependencies that would clash with this WebFlux app.
 */
public record GatewayErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        String traceId,
        List<Object> errors
) {
}
