package dev.batuhanyetgin.commonmodule.exception;

import dev.batuhanyetgin.commonmodule.model.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handle(BaseException ex, Locale locale, String logMessage) {

        log.info(logMessage);
        
        String formattedMessage = messageSource.getMessage(
                ex.getCode(),
                ex.getArgs(),
                ex.getArgs().length > 0 ? ex.getArgs()[0].toString() : "Hata oluştu",
                locale
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(formattedMessage));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handle(Exception ex, Locale locale) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Beklenmedik bir hata oluştu"));
    }
}