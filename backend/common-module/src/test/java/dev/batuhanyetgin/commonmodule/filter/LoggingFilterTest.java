package dev.batuhanyetgin.commonmodule.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingFilterTest {

    private final LoggingFilter filter = new LoggingFilter();

    private String maskSensitiveFields(final String body) {
        return (String) ReflectionTestUtils.invokeMethod(filter, "maskSensitiveFields", body);
    }

    private String extractBody(final byte[] content) {
        return (String) ReflectionTestUtils.invokeMethod(filter, "extractBody", content);
    }

    @Nested
    @DisplayName("maskSensitiveFields")
    class MaskSensitiveFields {

        @Test
        @DisplayName("password field is masked")
        void passwordField_masked() {
            final String body = "{\"email\":\"a@b.com\",\"password\":\"secret123\"}";

            assertThat(maskSensitiveFields(body))
                    .contains("\"password\":\"***\"")
                    .contains("\"email\":\"a@b.com\"")
                    .doesNotContain("secret123");
        }

        @Test
        @DisplayName("token field is masked, case-insensitively")
        void tokenField_maskedCaseInsensitive() {
            final String body = "{\"TOKEN\":\"abc.def.ghi\"}";

            assertThat(maskSensitiveFields(body)).doesNotContain("abc.def.ghi");
        }

        @Test
        @DisplayName("non-sensitive body is left unchanged")
        void nonSensitiveBody_unchanged() {
            final String body = "{\"debtName\":\"Rent\",\"amount\":100}";

            assertThat(maskSensitiveFields(body)).isEqualTo(body);
        }

        @Test
        @DisplayName("placeholder body '-' is returned as-is")
        void placeholderBody_returnedAsIs() {
            assertThat(maskSensitiveFields("-")).isEqualTo("-");
        }

        @Test
        @DisplayName("null body → null")
        void nullBody_returnsNull() {
            assertThat(maskSensitiveFields(null)).isNull();
        }
    }

    @Nested
    @DisplayName("extractBody")
    class ExtractBody {

        @Test
        @DisplayName("null content → placeholder")
        void nullContent_returnsPlaceholder() {
            assertThat(extractBody(null)).isEqualTo("-");
        }

        @Test
        @DisplayName("empty content → placeholder")
        void emptyContent_returnsPlaceholder() {
            assertThat(extractBody(new byte[0])).isEqualTo("-");
        }

        @Test
        @DisplayName("normal content → returned as trimmed string")
        void normalContent_returnedTrimmed() {
            assertThat(extractBody("  {\"a\":1}  ".getBytes())).isEqualTo("{\"a\":1}");
        }

        @Test
        @DisplayName("content over 2000 chars → truncated with suffix")
        void longContent_truncated() {
            final String longBody = "x".repeat(2500);

            final String result = extractBody(longBody.getBytes());

            assertThat(result).hasSize(2000 + "... [truncated]".length());
            assertThat(result).endsWith("... [truncated]");
        }
    }
}
