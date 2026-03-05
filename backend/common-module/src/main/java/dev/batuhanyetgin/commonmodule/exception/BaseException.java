package dev.batuhanyetgin.commonmodule.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BaseException extends RuntimeException {

    private final String code;
    private final String logMessage;
    private final Object[] args;
    private final HttpStatus status;

    public BaseException(String code, HttpStatus status, Object... args) {
        this(code, status, null, "", args);
    }

    public BaseException(String code, HttpStatus status, String logMessage, Object... args) {
        this(code, status, null, logMessage, args);
    }

    public BaseException(String code, HttpStatus status, Throwable cause, String logMessage, Object... args) {
        super(code, cause);
        this.code = code;
        this.status = status != null ? status : HttpStatus.BAD_REQUEST;
        this.logMessage = logMessage != null ? logMessage : "";
        this.args = args != null ? args : new Object[0];
    }

    public static BaseException badRequest(String code, Object... args) {
        return new BaseException(code, HttpStatus.BAD_REQUEST, args);
    }

    public static BaseException notFound(String code, Object... args) {
        return new BaseException(code, HttpStatus.NOT_FOUND, args);
    }

    public static BaseException conflict(String code, Object... args) {
        return new BaseException(code, HttpStatus.CONFLICT, args);
    }

    public static BaseException unauthorized(String code, Object... args) {
        return new BaseException(code, HttpStatus.UNAUTHORIZED, args);
    }

    public static BaseException forbidden(String code, Object... args) {
        return new BaseException(code, HttpStatus.FORBIDDEN, args);
    }

    public String getFormattedMessage(String pattern) {
        if (args == null || args.length == 0) {
            return pattern;
        }
        return java.text.MessageFormat.format(pattern, args);
    }

    public String getArgsAsString() {
        if (args == null || args.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            sb.append(arg).append(" ");
        }
        return sb.toString().trim();
    }
}