package dev.batuhanyetgin.authservice.exception;

import dev.batuhanyetgin.commonmodule.model.ErrorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class AuthExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(
            final JwtException ex, final Locale locale, final HttpServletRequest request) {
        log.warn("JwtException at path={}: {}", request.getRequestURI(), ex.getMessage());
        final String code = "error.auth.token_invalid";
        final String message = messageSource.getMessage(code, null, code, locale);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildError(request, HttpStatus.UNAUTHORIZED, code, message));
    }

    private ErrorResponse buildError(final HttpServletRequest request, final HttpStatus status,
                                     final String code, final String message) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .code(code)
                .message(message)
                .path(request.getRequestURI())
                .traceId(request.getHeader("X-Trace-Id"))
                .errors(List.of())
                .build();
    }
}
