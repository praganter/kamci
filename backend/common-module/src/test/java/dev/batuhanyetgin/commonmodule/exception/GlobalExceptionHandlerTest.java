package dev.batuhanyetgin.commonmodule.exception;

import dev.batuhanyetgin.commonmodule.model.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Locale;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private MessageSource messageSource;

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(messageSource);
        request = new MockHttpServletRequest();
        request.setRequestURI("/debt/123");
    }

    @Nested
    @DisplayName("handleBaseException")
    class HandleBaseException {

        @Test
        @DisplayName("uses exception's own HTTP status and localized message")
        void usesExceptionStatusAndLocalizedMessage() {
            when(messageSource.getMessage(any(), any(), any(), any(Locale.class)))
                    .thenReturn("Debt not found");
            final BaseException ex = BaseException.notFound("error.debt.not_found");

            final ResponseEntity<ErrorResponse> response =
                    handler.handleBaseException(ex, Locale.ENGLISH, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().getCode()).isEqualTo("error.debt.not_found");
            assertThat(response.getBody().getMessage()).isEqualTo("Debt not found");
            assertThat(response.getBody().getPath()).isEqualTo("/debt/123");
        }

        @Test
        @DisplayName("null status on exception → defaults to BAD_REQUEST")
        void nullStatus_defaultsToBadRequest() {
            when(messageSource.getMessage(any(), any(), any(), any(Locale.class))).thenReturn("msg");
            final BaseException ex = new BaseException("error.code", null);

            final ResponseEntity<ErrorResponse> response =
                    handler.handleBaseException(ex, Locale.ENGLISH, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("handleCompletionException — unwraps cause")
    class HandleCompletionException {

        @Test
        @DisplayName("cause is BaseException → delegates to handleBaseException's status/code")
        void causeIsBaseException_delegates() {
            when(messageSource.getMessage(any(), any(), any(), any(Locale.class))).thenReturn("msg");
            final CompletionException ex = new CompletionException(BaseException.notFound("error.x"));

            final ResponseEntity<?> response = handler.handleCompletionException(ex, Locale.ENGLISH, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("cause is DuplicateKeyException → 409 CONFLICT")
        void causeIsDuplicateKey_conflict() {
            when(messageSource.getMessage(any(), any(), any(), any(Locale.class))).thenReturn("msg");
            final CompletionException ex = new CompletionException(new DuplicateKeyException("dup"));

            final ResponseEntity<?> response = handler.handleCompletionException(ex, Locale.ENGLISH, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("cause is IllegalArgumentException → 400 BAD_REQUEST")
        void causeIsIllegalArgument_badRequest() {
            when(messageSource.getMessage(any(), any(), any(), any(Locale.class))).thenReturn("msg");
            final CompletionException ex = new CompletionException(new IllegalArgumentException("bad"));

            final ResponseEntity<?> response = handler.handleCompletionException(ex, Locale.ENGLISH, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("cause is an unrecognized exception → falls through to generic handler (500)")
        void causeIsUnrecognized_fallsThroughToGeneric() {
            when(messageSource.getMessage(any(), any(), any(), any(Locale.class))).thenReturn("msg");
            final CompletionException ex = new CompletionException(new RuntimeException("boom"));

            final ResponseEntity<?> response = handler.handleCompletionException(ex, Locale.ENGLISH, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("handleAny — generic classification")
    class HandleAny {

        @Test
        @DisplayName("StringIndexOutOfBoundsException → 400 with invalid_header code")
        void stringIndexOutOfBounds_badRequest() {
            when(messageSource.getMessage(any(), any(), any(), any(Locale.class))).thenReturn("msg");

            final ResponseEntity<ErrorResponse> response =
                    handler.handleAny(new StringIndexOutOfBoundsException(), request, Locale.ENGLISH);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getCode()).isEqualTo("error.request.invalid_header");
        }

        @Test
        @DisplayName("IllegalStateException → 400 with illegal_state code")
        void illegalState_badRequest() {
            when(messageSource.getMessage(any(), any(), any(), any(Locale.class))).thenReturn("msg");

            final ResponseEntity<ErrorResponse> response =
                    handler.handleAny(new IllegalStateException(), request, Locale.ENGLISH);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getCode()).isEqualTo("error.illegal_state");
        }

        @Test
        @DisplayName("unrecognized exception → 500 with unexpected code")
        void unrecognized_internalServerError() {
            when(messageSource.getMessage(any(), any(), any(), any(Locale.class))).thenReturn("msg");

            final ResponseEntity<ErrorResponse> response =
                    handler.handleAny(new RuntimeException("boom"), request, Locale.ENGLISH);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().getCode()).isEqualTo("error.unexpected");
        }
    }

    @Nested
    @DisplayName("other dedicated handlers")
    class OtherHandlers {

        @Test
        @DisplayName("handleDuplicateKey → 409 CONFLICT")
        void handleDuplicateKey_conflict() {
            when(messageSource.getMessage(any(), any(), any(), any(Locale.class))).thenReturn("msg");

            final ResponseEntity<ErrorResponse> response =
                    handler.handleDuplicateKey(new DuplicateKeyException("dup"), request, Locale.ENGLISH);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("handleDataAccess → 500 INTERNAL_SERVER_ERROR")
        void handleDataAccess_internalServerError() {
            when(messageSource.getMessage(any(), any(), any(), any(Locale.class))).thenReturn("msg");
            final DataAccessException ex = new DataAccessException("db down") {
            };

            final ResponseEntity<ErrorResponse> response = handler.handleDataAccess(ex, request, Locale.ENGLISH);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("handleIllegalArgument → 400 BAD_REQUEST")
        void handleIllegalArgument_badRequest() {
            when(messageSource.getMessage(any(), any(), any(), any(Locale.class))).thenReturn("msg");

            final ResponseEntity<ErrorResponse> response =
                    handler.handleIllegalArgument(new IllegalArgumentException("bad"), request, Locale.ENGLISH);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

}
