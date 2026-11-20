package dev.batuhanyetgin.commonmodule.config;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/** Service-level guard for {@code internal} endpoints, in addition to the gateway block. */
@Configuration
public class InternalApiConfig implements WebMvcConfigurer {

    public static final String HEADER = "X-Internal-Token";
    public static final String DEV_DEFAULT_TOKEN = "dev-internal-token";

    private static final Logger log = LoggerFactory.getLogger(InternalApiConfig.class);

    private final String token;

    public InternalApiConfig(@Value("${INTERNAL_API_TOKEN:" + DEV_DEFAULT_TOKEN + "}") final String token) {
        this.token = token;
        if (DEV_DEFAULT_TOKEN.equals(token)) {
            log.warn("INTERNAL_API_TOKEN is not set, using the public dev default; set it outside local development");
        }
    }

    @Override
    public void addInterceptors(@NonNull final InterceptorRegistry registry) {
        registry.addInterceptor(new InternalTokenInterceptor(token));
    }

    public static class InternalTokenInterceptor implements HandlerInterceptor {

        private final byte[] expected;

        public InternalTokenInterceptor(final String token) {
            this.expected = token.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public boolean preHandle(@NonNull final HttpServletRequest request,
                                 @NonNull final HttpServletResponse response,
                                 @NonNull final Object handler) {
            if (!isInternal(request)) {
                return true;
            }
            final String given = request.getHeader(HEADER);
            if (expected.length == 0 || given == null
                    || !MessageDigest.isEqual(expected, given.getBytes(StandardCharsets.UTF_8))) {
                throw BaseException.forbidden("error.internal.unauthorized");
            }
            return true;
        }

        // Matched mapping pattern first: the raw request URI can carry percent-encoded segments
        // (inter%6Eal) that Spring decodes before routing.
        private static boolean isInternal(final HttpServletRequest request) {
            final Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            final String path = pattern != null
                    ? pattern.toString()
                    : UriUtils.decode(request.getRequestURI(), StandardCharsets.UTF_8);
            return Arrays.asList(path.split("/")).contains("internal");
        }
    }
}
