package dev.batuhanyetgin.commonmodule.exception;

import dev.batuhanyetgin.commonmodule.model.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Spring-MVC-facing handlers of {@link GlobalExceptionHandler}. */
class GlobalExceptionHandlerFrameworkTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        final StaticMessageSource messages = new StaticMessageSource();
        messages.setUseCodeAsDefaultMessage(true);
        handler = new GlobalExceptionHandler(messages);
        request = new MockHttpServletRequest();
        request.setRequestURI("/debt/123");
    }

    private static HttpMessageNotReadableException notReadable() {
        return new HttpMessageNotReadableException("bad json", new MockHttpInputMessage(new byte[0]));
    }

    @Test
    @DisplayName("bean validation failure → 422 with one error per field")
    void methodArgumentNotValid() throws Exception {
        final BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "body");
        binding.addError(new FieldError("body", "email", "must not be blank"));
        final MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                new MethodParameter(Object.class.getMethod("toString"), -1), binding);

        final ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.UNPROCESSABLE_ENTITY, new ServletWebRequest(request));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        final ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body.getErrors()).hasSize(1);
        assertThat(body.getErrors().get(0).getField()).isEqualTo("email");
        assertThat(body.getPath()).isEqualTo("/debt/123");
    }

    @Test
    @DisplayName("unreadable body → 400")
    void notReadable400() {
        final ResponseEntity<Object> response = handler.handleHttpMessageNotReadable(
                notReadable(), new HttpHeaders(), HttpStatus.BAD_REQUEST, new ServletWebRequest(request));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("missing request parameter → 400")
    void missingParameter() {
        final ResponseEntity<Object> response = handler.handleMissingServletRequestParameter(
                new MissingServletRequestParameterException("isOwner", "boolean"),
                new HttpHeaders(), HttpStatus.BAD_REQUEST, new ServletWebRequest(request));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("non-servlet web request: path comes from the description, trace id stays null")
    void nonServletRequestPath() {
        final WebRequest web = mock(WebRequest.class);
        when(web.getDescription(false)).thenReturn("uri=/auth/login");

        final ErrorResponse body = (ErrorResponse) handler.handleHttpMessageNotReadable(
                notReadable(), new HttpHeaders(), HttpStatus.BAD_REQUEST, web).getBody();

        assertThat(body.getPath()).isEqualTo("/auth/login");
        assertThat(body.getTraceId()).isNull();
    }

    @Test
    @DisplayName("non-servlet web request with an unknown description → path null")
    void nonServletRequestUnknownDescription() {
        final WebRequest web = mock(WebRequest.class);
        when(web.getDescription(false)).thenReturn("client=1.2.3.4");

        final ErrorResponse body = (ErrorResponse) handler.handleHttpMessageNotReadable(
                notReadable(), new HttpHeaders(), HttpStatus.BAD_REQUEST, web).getBody();

        assertThat(body.getPath()).isNull();
    }

    @Test
    @DisplayName("argument type mismatch → 400")
    void typeMismatch() {
        final MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("abc", Integer.class, "page", null, null);

        assertThat(handler.handleTypeMismatch(ex, request, Locale.ENGLISH).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("constraint violation → 422, tolerating a violation without a path")
    void constraintViolation() {
        final ConstraintViolation<?> withPath = mock(ConstraintViolation.class);
        final Path path = mock(Path.class);
        when(path.toString()).thenReturn("amount");
        when(withPath.getPropertyPath()).thenReturn(path);
        final ConstraintViolation<?> withoutPath = mock(ConstraintViolation.class);
        when(withoutPath.getPropertyPath()).thenReturn(null);

        final ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(
                new ConstraintViolationException(Set.of(withPath, withoutPath)), request, Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().getErrors()).hasSize(2);
    }

    @Test
    @DisplayName("CompletionException wrapping a DataAccessException → 500")
    void completionWrappingDataAccess() {
        final ResponseEntity<?> response = handler.handleCompletionException(
                new CompletionException(new DataAccessException("db") { }), Locale.ENGLISH, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
