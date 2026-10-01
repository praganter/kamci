package dev.batuhanyetgin.commonmodule.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeUtilTest {

    @Test
    @DisplayName("null → empty string")
    void nullDateTime_returnsEmptyString() {
        assertThat(DateTimeUtil.format(null)).isEmpty();
    }

    @Test
    @DisplayName("formats as dd.MM.yyyy HH:mm")
    void formatsAsDisplayPattern() {
        final LocalDateTime dateTime = LocalDateTime.of(2026, 4, 6, 9, 5);

        assertThat(DateTimeUtil.format(dateTime)).isEqualTo("06.04.2026 09:05");
    }
}
