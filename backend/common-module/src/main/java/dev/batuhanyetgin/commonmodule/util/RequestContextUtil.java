package dev.batuhanyetgin.commonmodule.util;

import dev.batuhanyetgin.commonmodule.constant.CommonConstants;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

public final class RequestContextUtil {

    private RequestContextUtil() {
    }

    public static String getUserId() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .map(request -> request.getHeader(CommonConstants.USER_ID_HEADER))
                .orElseThrow(() -> new BaseException("error.user.not_found_in_context", HttpStatus.UNAUTHORIZED));
    }

    public static boolean isPremium() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .map(request -> request.getHeader(CommonConstants.PREMIUM_HEADER))
                .map("true"::equalsIgnoreCase)
                .orElse(false);
    }

    public static String getLocale() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .map(request -> request.getHeader(CommonConstants.ACCEPT_LANGUAGE_HEADER))
                .filter(lang -> lang.startsWith(CommonConstants.LOCALE_EN))
                .map(lang -> CommonConstants.LOCALE_EN)
                .orElse(CommonConstants.DEFAULT_LOCALE);
    }
}