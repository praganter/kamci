package dev.batuhanyetgin.kamci.converter.read;

import com.mongodb.lang.NonNull;
import dev.batuhanyetgin.kamci.enums.PaymentPeriodType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;

@Component
@ReadingConverter
public class PaymentPeriodTypeReadConverter implements Converter<Character, PaymentPeriodType> {

    @Override
    public PaymentPeriodType convert(final @NonNull Character source) {
        for (PaymentPeriodType type : PaymentPeriodType.values()) {
            if (type.getValue().equals(source)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown PaymentPeriodType: " + source);
    }
}