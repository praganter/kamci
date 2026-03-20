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
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionException;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<?> handleCompletionException(
            final CompletionException ex, final Locale locale, final HttpServletRequest request) {
        final Throwable cause = ex.getCause();
        if (cause instanceof BaseException baseEx) {
            return handleBaseException(baseEx, locale, request);
        }
        if (cause instanceof DuplicateKeyException dke) {
            return handleDuplicateKey(dke, request, locale);
        }
        if (cause instanceof DataAccessException dae) {
            return handleDataAccess(dae, request, locale);
        }
        if (cause instanceof IllegalArgumentException iae) {
            return handleIllegalArgument(iae, request, locale);
        }
        return handleAny(cause instanceof Exception e ? e : ex, request, locale);
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(
            final BaseException ex, final Locale locale, final HttpServletRequest request) {
        final HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.BAD_REQUEST;
        if (!ex.getLogMessage().isBlank()) {
            log.warn("BaseException: status={}, code={}, msg={}", status.value(), ex.getCode(), ex.getLogMessage(), ex);
        } else {
            log.warn("BaseException: status={}, code={}", status.value(), ex.getCode(), ex);
        }
        final String localizedMessage = messageSource.getMessage(
                ex.getCode(), ex.getArgs(), msg("error.default", locale), locale);
        return ResponseEntity.status(status)
                .body(buildError(request, status, ex.getCode(), localizedMessage, null));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException ex, final HttpHeaders headers,
            final HttpStatusCode status, final WebRequest request) {
        final List<ErrorResponse.ValidationError> errors = ex.getBindingResult()
                .getFieldErrors().stream().map(this::toValidationError).toList();
        final String code = "error.validation";
        return ResponseEntity.unprocessableEntity()
                .body(buildError(request, HttpStatus.UNPROCESSABLE_ENTITY, code, msg(code, request.getLocale()), errors));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            final HttpMessageNotReadableException ex, final HttpHeaders headers,
            final HttpStatusCode status, final WebRequest request) {
        log.warn("Body not readable: {}", ex.getMessage());
        final String code = "error.request.body_not_readable";
        return ResponseEntity.badRequest()
                .body(buildError(request, HttpStatus.BAD_REQUEST, code, msg(code, request.getLocale()), null));
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            final MissingServletRequestParameterException ex, final HttpHeaders headers,
            final HttpStatusCode status, final WebRequest request) {
        final String code = "error.request.missing_parameter";
        return ResponseEntity.badRequest()
                .body(buildError(request, HttpStatus.BAD_REQUEST, code,
                        msg(code, request.getLocale(), ex.getParameterName()), null));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            final MethodArgumentTypeMismatchException ex,
            final HttpServletRequest request, final Locale locale) {
        final String code = "error.request.type_mismatch";
        return ResponseEntity.badRequest()
                .body(buildError(request, HttpStatus.BAD_REQUEST, code, msg(code, locale, ex.getName()), null));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            final ConstraintViolationException ex,
            final HttpServletRequest request, final Locale locale) {
        final List<ErrorResponse.ValidationError> errors = ex.getConstraintViolations()
                .stream().map(this::toValidationError).toList();
        final String code = "error.validation";
        return ResponseEntity.unprocessableEntity()
                .body(buildError(request, HttpStatus.UNPROCESSABLE_ENTITY, code, msg(code, locale), errors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            final IllegalArgumentException ex,
            final HttpServletRequest request, final Locale locale) {
        log.warn("IllegalArgumentException at path={}: {}", request.getRequestURI(), ex.getMessage());
        final String code = "error.illegal_argument";
        return ResponseEntity.badRequest()
                .body(buildError(request, HttpStatus.BAD_REQUEST, code, msg(code, locale), null));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateKey(
            final DuplicateKeyException ex,
            final HttpServletRequest request, final Locale locale) {
        log.warn("DuplicateKeyException at path={}", request.getRequestURI(), ex);
        final String code = "error.db.duplicate_key";
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(buildError(request, HttpStatus.CONFLICT, code, msg(code, locale), null));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(
            final DataAccessException ex,
            final HttpServletRequest request, final Locale locale) {
        log.error("DataAccessException at path={}", request.getRequestURI(), ex);
        final String code = "error.db";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(request, HttpStatus.INTERNAL_SERVER_ERROR, code, msg(code, locale), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(
            final Exception ex, final HttpServletRequest request, final Locale locale) {
        final ErrorSpec spec = classify(ex, locale);
        if (spec.status().is5xxServerError()) {
            log.error("Unexpected error: path={}", request.getRequestURI(), ex);
        } else {
            log.warn("Request error: path={}, type={}, msg={}",
                    request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage());
        }
        return ResponseEntity.status(spec.status())
                .body(buildError(request, spec.status(), spec.code(), spec.message(), null));
    }

    private ErrorSpec classify(final Exception ex, final Locale locale) {
        if (ex instanceof StringIndexOutOfBoundsException) {
            final String code = "error.request.invalid_header";
            return new ErrorSpec(HttpStatus.BAD_REQUEST, code, msg(code, locale));
        }
        if (ex instanceof IllegalStateException) {
            final String code = "error.illegal_state";
            return new ErrorSpec(HttpStatus.BAD_REQUEST, code, msg(code, locale));
        }
        final String code = "error.unexpected";
        return new ErrorSpec(HttpStatus.INTERNAL_SERVER_ERROR, code, msg(code, locale));
    }

    private String msg(final String code, final Locale locale, final Object... args) {
        return messageSource.getMessage(code, args, code, locale);
    }

    private ErrorResponse.ValidationError toValidationError(final FieldError fe) {
        return ErrorResponse.ValidationError.builder()
                .field(fe.getField())
                .message(fe.getDefaultMessage())
                .build();
    }

    private ErrorResponse.ValidationError toValidationError(final ConstraintViolation<?> v) {
        final String field = v.getPropertyPath() != null ? v.getPropertyPath().toString() : null;
        return ErrorResponse.ValidationError.builder()
                .field(field).message(v.getMessage()).build();
    }

    private ErrorResponse buildError(final HttpServletRequest request, final HttpStatus status,
                                     final String code, final String message,
                                     final List<ErrorResponse.ValidationError> errors) {
        return ErrorResponse.builder()
                .timestamp(Instant.now()).status(status.value()).code(code)
                .message(message).path(request.getRequestURI())
                .traceId(request.getHeader("X-Trace-Id")).errors(errors).build();
    }

    private ErrorResponse buildError(final WebRequest request, final HttpStatus status,
                                     final String code, final String message,
                                     final List<ErrorResponse.ValidationError> errors) {
        String path = null;
        String traceId = null;
        if (request instanceof ServletWebRequest swr) {
            path = swr.getRequest().getRequestURI();
            traceId = swr.getRequest().getHeader("X-Trace-Id");
        } else {
            final String desc = request.getDescription(false);
            if (desc != null && desc.startsWith("uri=")) {
                path = desc.substring(4);
            }
        }
        return ErrorResponse.builder()
                .timestamp(Instant.now()).status(status.value()).code(code)
                .message(message).path(path).traceId(traceId).errors(errors).build();
    }

    private record ErrorSpec(HttpStatus status, String code, String message) {
    }
}