package dev.batuhanyetgin.commonmodule.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
public class BaseException extends RuntimeException {

    private final String code;
    private final String logMessage;
    private final Object[] args;
    private final HttpStatus status;

    public BaseException(final String code, final HttpStatus status, final Object... args) {
        this(code, status, null, "", args);
    }

    public BaseException(final String code, final HttpStatus status,
                         final String logMessage, final Object... args) {
        this(code, status, null, logMessage, args);
    }

    public BaseException(final String code, final HttpStatus status,
                         final Throwable cause, final String logMessage, final Object... args) {
        super(code, cause);
        this.code = code;
        this.status = status != null ? status : HttpStatus.BAD_REQUEST;
        this.logMessage = logMessage != null ? logMessage : "";
        this.args = args != null ? args : new Object[0];
    }

    public static BaseException badRequest(final String code, final Object... args) {
        return new BaseException(code, HttpStatus.BAD_REQUEST, args);
    }

    public static BaseException notFound(final String code, final Object... args) {
        return new BaseException(code, HttpStatus.NOT_FOUND, args);
    }

    public static BaseException conflict(final String code, final Object... args) {
        return new BaseException(code, HttpStatus.CONFLICT, args);
    }

    public static BaseException unauthorized(final String code, final Object... args) {
        return new BaseException(code, HttpStatus.UNAUTHORIZED, args);
    }

    public static BaseException forbidden(final String code, final Object... args) {
        return new BaseException(code, HttpStatus.FORBIDDEN, args);
    }

    public static BaseException serviceUnavailable(final String code, final Object... args) {
        return new BaseException(code, HttpStatus.SERVICE_UNAVAILABLE, args);
    }

    public String getFormattedMessage(final String pattern) {
        if (args == null || args.length == 0) {
            return pattern;
        }
        return java.text.MessageFormat.format(pattern, args);
    }

    public String getArgsAsString() {
        if (args == null || args.length == 0) {
            return "";
        }
        return Arrays.stream(args)
                .map(Object::toString)
                .collect(Collectors.joining(" "));
    }
}