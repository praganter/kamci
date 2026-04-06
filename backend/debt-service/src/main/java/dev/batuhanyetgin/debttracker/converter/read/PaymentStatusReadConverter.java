package dev.batuhanyetgin.debttracker.converter.read;

import com.mongodb.lang.NonNull;
import dev.batuhanyetgin.debttracker.enums.PaymentStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;

@Component
@ReadingConverter
public class PaymentStatusReadConverter implements Converter<Character, PaymentStatus> {

    @Override
    public PaymentStatus convert(final @NonNull Character source) {
        for (PaymentStatus status : PaymentStatus.values()) {
            if (status.getValue().equals(source)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown PaymentStatus: " + source);
    }
}
