package dev.batuhanyetgin.commonmodule.exception;

import dev.batuhanyetgin.commonmodule.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex, Locale locale, HttpServletRequest request) {
        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.BAD_REQUEST;

        if (!ex.getLogMessage().isBlank()) {
            log.warn("BaseException: status={}, code={}, msg={}", status.value(), ex.getCode(), ex.getLogMessage(), ex);
        } else {
            log.warn("BaseException: status={}, code={}", status.value(), ex.getCode(), ex);
        }

        String localizedMessage = messageSource.getMessage(
                ex.getCode(),
                ex.getArgs(),
                msg("error.default", locale),
                locale
        );

        return ResponseEntity.status(status).body(buildError(
                request,
                status,
                ex.getCode(),
                localizedMessage,
                null
        ));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        List<ErrorResponse.ValidationError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toValidationError)
                .toList();

        Locale locale = request.getLocale();
        String code = "error.validation";
        String message = msg(code, locale);

        ErrorResponse body = buildError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                code,
                message,
                errors
        );

        return ResponseEntity.unprocessableEntity().body(body);
    }


    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        log.warn("Body not readable: {}", ex.getMessage());

        Locale locale = request.getLocale();
        String code = "error.request.body_not_readable";
        String message = msg(code, locale);

        ErrorResponse body = buildError(
                request,
                HttpStatus.BAD_REQUEST,
                code,
                message,
                null
        );

        return ResponseEntity.badRequest().body(body);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Locale locale = request.getLocale();
        String code = "error.request.missing_parameter";
        String message = msg(code, locale, ex.getParameterName());

        ErrorResponse body = buildError(
                request,
                HttpStatus.BAD_REQUEST,
                code,
                message,
                null
        );

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request, Locale locale) {
        String code = "error.request.type_mismatch";
        String message = msg(code, locale, ex.getName());

        ErrorResponse body = buildError(
                request,
                HttpStatus.BAD_REQUEST,
                code,
                message,
                null
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request, Locale locale) {

        List<ErrorResponse.ValidationError> errors = ex.getConstraintViolations().stream()
                .map(this::toValidationError)
                .toList();

        String code = "error.validation";
        String message = msg(code, locale);

        ErrorResponse body = buildError(
                request,
                HttpStatus.UNPROCESSABLE_ENTITY,
                code,
                message,
                errors
        );
        return ResponseEntity.unprocessableEntity().body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request, Locale locale) {
        log.warn("IllegalArgumentException at path={}: {}", request.getRequestURI(), ex.getMessage());

        String code = "error.illegal_argument";
        String message = msg(code, locale);

        ErrorResponse body = buildError(
                request,
                HttpStatus.BAD_REQUEST,
                code,
                message,
                null
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateKey(DuplicateKeyException ex, HttpServletRequest request, Locale locale) {
        log.warn("DuplicateKeyException at path={}", request.getRequestURI(), ex);

        String code = "error.db.duplicate_key";
        String message = msg(code, locale);

        ErrorResponse body = buildError(
                request,
                HttpStatus.CONFLICT,
                code,
                message,
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException ex, HttpServletRequest request, Locale locale) {
        log.error("DataAccessException at path={}", request.getRequestURI(), ex);

        String code = "error.db";
        String message = msg(code, locale);

        ErrorResponse body = buildError(
                request,
                HttpStatus.INTERNAL_SERVER_ERROR,
                code,
                message,
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception ex, HttpServletRequest request, Locale locale) {
        ErrorSpec spec = classify(ex, locale);

        if (spec.status.is5xxServerError()) {
            log.error("Unexpected error: path={}", request.getRequestURI(), ex);
        } else {
            log.warn("Request error: path={}, type={}, msg={}", request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage());
        }

        ErrorResponse body = buildError(
                request,
                spec.status,
                spec.code,
                spec.message,
                null
        );

        return ResponseEntity.status(spec.status).body(body);
    }

    private ErrorSpec classify(Exception ex, Locale locale) {
        if (ex instanceof StringIndexOutOfBoundsException) {
            String code = "error.request.invalid_header";
            return new ErrorSpec(HttpStatus.BAD_REQUEST, code, msg(code, locale));
        }
        if (ex instanceof IllegalStateException) {
            String code = "error.illegal_state";
            return new ErrorSpec(HttpStatus.BAD_REQUEST, code, msg(code, locale));
        }

        String code = "error.unexpected";
        return new ErrorSpec(HttpStatus.INTERNAL_SERVER_ERROR, code, msg(code, locale));
    }

    private String msg(String code, Locale locale, Object... args) {
        return messageSource.getMessage(code, args, code, locale);
    }

    private ErrorResponse.ValidationError toValidationError(FieldError fe) {
        return ErrorResponse.ValidationError.builder()
                .field(fe.getField())
                .message(fe.getDefaultMessage())
                .build();
    }

    private ErrorResponse.ValidationError toValidationError(ConstraintViolation<?> v) {
        String field = v.getPropertyPath() != null ? v.getPropertyPath().toString() : null;
        return ErrorResponse.ValidationError.builder()
                .field(field)
                .message(v.getMessage())
                .build();
    }

    private ErrorResponse buildError(
            HttpServletRequest request,
            HttpStatus status,
            String code,
            String message,
            List<ErrorResponse.ValidationError> errors) {

        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .code(code)
                .message(message)
                .path(request.getRequestURI())
                .traceId(request.getHeader("X-Trace-Id"))
                .errors(errors)
                .build();
    }

    private ErrorResponse buildError(
            WebRequest request,
            HttpStatus status,
            String code,
            String message,
            List<ErrorResponse.ValidationError> errors) {

        String path = null;
        String traceId = null;

        if (request instanceof ServletWebRequest swr) {
            path = swr.getRequest().getRequestURI();
            traceId = swr.getRequest().getHeader("X-Trace-Id");
        } else {
            String desc = request.getDescription(false);
            if (desc != null && desc.startsWith("uri=")) {
                path = desc.substring(4);
            }
        }

        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .code(code)
                .message(message)
                .path(path)
                .traceId(traceId)
                .errors(errors)
                .build();
    }

    private record ErrorSpec(HttpStatus status, String code, String message) {}
}