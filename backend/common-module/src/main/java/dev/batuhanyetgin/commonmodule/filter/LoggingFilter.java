package dev.batuhanyetgin.commonmodule.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@Order(1)
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    private static final int MAX_BODY_LENGTH = 2000;

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "newPassword", "oldPassword", "confirmPassword",
            "token", "refreshToken", "secret"
    );

    private static final Pattern SENSITIVE_PATTERN = buildSensitivePattern();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        final ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        final long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            final long duration = System.currentTimeMillis() - startTime;

            final String requestBody = extractBody(wrappedRequest.getContentAsByteArray());
            final String responseBody = extractBody(wrappedResponse.getContentAsByteArray());

            log.debug(
                    "\n[REQUEST]  {} {}\n           Body: {}\n[RESPONSE] {} ({}ms)\n           Body: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    maskSensitiveFields(requestBody),
                    wrappedResponse.getStatus(),
                    duration,
                    maskSensitiveFields(responseBody)
            );

            wrappedResponse.copyBodyToResponse();
        }
    }

    private String extractBody(final byte[] content) {
        if (content == null || content.length == 0) {
            return "-";
        }
        final String body = new String(content, StandardCharsets.UTF_8).trim();
        if (body.isEmpty()) {
            return "-";
        }
        if (body.length() > MAX_BODY_LENGTH) {
            return body.substring(0, MAX_BODY_LENGTH) + "... [truncated]";
        }
        return body;
    }

    private String maskSensitiveFields(final String body) {
        if (body == null || body.equals("-")) {
            return body;
        }
        return SENSITIVE_PATTERN.matcher(body).replaceAll("\"$1\":\"***\"");
    }

    private static Pattern buildSensitivePattern() {
        final String fields = String.join("|", SENSITIVE_FIELDS);
        return Pattern.compile("\"(" + fields + ")\"\\s*:\\s*\"[^\"]*\"", Pattern.CASE_INSENSITIVE);
    }
}