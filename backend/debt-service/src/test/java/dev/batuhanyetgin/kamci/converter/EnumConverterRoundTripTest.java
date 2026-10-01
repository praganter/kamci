package dev.batuhanyetgin.kamci.converter;

import dev.batuhanyetgin.kamci.converter.read.AccessLevelTypeReadConverter;
import dev.batuhanyetgin.kamci.converter.read.DebtStatusReadConverter;
import dev.batuhanyetgin.kamci.converter.read.PaymentPeriodTypeReadConverter;
import dev.batuhanyetgin.kamci.converter.read.PaymentStatusReadConverter;
import dev.batuhanyetgin.kamci.converter.write.AccessLevelTypeWriteConverter;
import dev.batuhanyetgin.kamci.converter.write.DebtStatusWriteConverter;
import dev.batuhanyetgin.kamci.converter.write.PaymentPeriodTypeWriteConverter;
import dev.batuhanyetgin.kamci.converter.write.PaymentStatusWriteConverter;
import dev.batuhanyetgin.kamci.enums.AccessLevelType;
import dev.batuhanyetgin.kamci.enums.DebtStatus;
import dev.batuhanyetgin.kamci.enums.PaymentPeriodType;
import dev.batuhanyetgin.kamci.enums.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Each Mongo read/write converter pair is a 1:1 enum<->Character mapping. Round-trips every
 * enum value through write-then-read and verifies an unmapped char fails loudly on read,
 * rather than silently persisting the wrong status.
 */
class EnumConverterRoundTripTest {

    @Nested
    @DisplayName("AccessLevelType")
    class AccessLevelTypeConverter {

        private final AccessLevelTypeWriteConverter writeConverter = new AccessLevelTypeWriteConverter();
        private final AccessLevelTypeReadConverter readConverter = new AccessLevelTypeReadConverter();

        @ParameterizedTest
        @EnumSource(AccessLevelType.class)
        @DisplayName("round-trips every enum value through write then read")
        void roundTrips(final AccessLevelType value) {
            assertThat(readConverter.convert(writeConverter.convert(value))).isEqualTo(value);
        }

        @org.junit.jupiter.api.Test
        @DisplayName("unmapped char → throws IllegalArgumentException")
        void unmappedChar_throws() {
            assertThatThrownBy(() -> readConverter.convert('Z')).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("DebtStatus")
    class DebtStatusConverter {

        private final DebtStatusWriteConverter writeConverter = new DebtStatusWriteConverter();
        private final DebtStatusReadConverter readConverter = new DebtStatusReadConverter();

        @ParameterizedTest
        @EnumSource(DebtStatus.class)
        @DisplayName("round-trips every enum value through write then read")
        void roundTrips(final DebtStatus value) {
            assertThat(readConverter.convert(writeConverter.convert(value))).isEqualTo(value);
        }

        @org.junit.jupiter.api.Test
        @DisplayName("unmapped char → throws IllegalArgumentException")
        void unmappedChar_throws() {
            assertThatThrownBy(() -> readConverter.convert('Z')).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("PaymentPeriodType")
    class PaymentPeriodTypeConverter {

        private final PaymentPeriodTypeWriteConverter writeConverter = new PaymentPeriodTypeWriteConverter();
        private final PaymentPeriodTypeReadConverter readConverter = new PaymentPeriodTypeReadConverter();

        @ParameterizedTest
        @EnumSource(PaymentPeriodType.class)
        @DisplayName("round-trips every enum value through write then read")
        void roundTrips(final PaymentPeriodType value) {
            assertThat(readConverter.convert(writeConverter.convert(value))).isEqualTo(value);
        }

        @org.junit.jupiter.api.Test
        @DisplayName("unmapped char → throws IllegalArgumentException")
        void unmappedChar_throws() {
            assertThatThrownBy(() -> readConverter.convert('Z')).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("PaymentStatus")
    class PaymentStatusConverter {

        private final PaymentStatusWriteConverter writeConverter = new PaymentStatusWriteConverter();
        private final PaymentStatusReadConverter readConverter = new PaymentStatusReadConverter();

        @ParameterizedTest
        @EnumSource(PaymentStatus.class)
        @DisplayName("round-trips every enum value through write then read")
        void roundTrips(final PaymentStatus value) {
            assertThat(readConverter.convert(writeConverter.convert(value))).isEqualTo(value);
        }

        @org.junit.jupiter.api.Test
        @DisplayName("unmapped char → throws IllegalArgumentException")
        void unmappedChar_throws() {
            assertThatThrownBy(() -> readConverter.convert('Z')).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
