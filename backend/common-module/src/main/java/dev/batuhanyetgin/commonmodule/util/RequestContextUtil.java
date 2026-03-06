package dev.batuhanyetgin.commonmodule.util;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

public final class RequestContextUtil {

    private static final String USER_ID_HEADER = "user-id";

    private RequestContextUtil() {
    }

    public static String getUserId() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .map(request -> request.getHeader(USER_ID_HEADER))
                .orElseThrow(() -> new BaseException("error.user.not_found_in_context", HttpStatus.UNAUTHORIZED));
    }
}