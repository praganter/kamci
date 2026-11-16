package dev.batuhanyetgin.commonmodule.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class BaseExceptionTest {

    @Test
    @DisplayName("factory methods map to their HTTP status")
    void factories() {
        assertThat(BaseException.badRequest("c").getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(BaseException.notFound("c").getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(BaseException.conflict("c").getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(BaseException.unauthorized("c").getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(BaseException.forbidden("c").getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(BaseException.tooManyRequests("c").getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(BaseException.serviceUnavailable("c").getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("null status, log message and args fall back to safe defaults")
    void nullDefaults() {
        final BaseException ex = new BaseException("c", null, new RuntimeException("x"), null, (Object[]) null);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getLogMessage()).isEmpty();
        assertThat(ex.getArgs()).isEmpty();
        assertThat(ex.getCause()).hasMessage("x");
    }

    @Test
    @DisplayName("log-message constructor keeps the message and args")
    void logMessageConstructor() {
        final BaseException ex = new BaseException("c", HttpStatus.CONFLICT, "details", 1, "a");

        assertThat(ex.getLogMessage()).isEqualTo("details");
        assertThat(ex.getArgsAsString()).isEqualTo("1 a");
    }

    @Test
    @DisplayName("formatted message and args string handle empty and non-empty args")
    void formatting() {
        assertThat(BaseException.badRequest("c").getFormattedMessage("plain")).isEqualTo("plain");
        assertThat(BaseException.badRequest("c").getArgsAsString()).isEmpty();
        assertThat(BaseException.badRequest("c", "x", 2).getFormattedMessage("{0}-{1}")).isEqualTo("x-2");
    }
}
