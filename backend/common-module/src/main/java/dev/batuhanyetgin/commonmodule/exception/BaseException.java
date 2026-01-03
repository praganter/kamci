package dev.batuhanyetgin.commonmodule.exception;

import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {

    private final String code;
    private final String logMessage;
    private final Object[] args;

    protected BaseException(String code, String logMessage, Object... args) {
        super(code);
        this.code = code;
        this.args = args;
        this.logMessage = logMessage;
    }

    public BaseException(String code) {
        super(code);
        this.code = code;
        this.logMessage = "";
        this.args = new Object[0];
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