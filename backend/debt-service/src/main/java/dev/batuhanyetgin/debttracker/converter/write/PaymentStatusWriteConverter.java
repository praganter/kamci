package dev.batuhanyetgin.debttracker.converter.write;

import dev.batuhanyetgin.debttracker.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.stereotype.Component;

@Component
@WritingConverter
public class PaymentStatusWriteConverter implements Converter<PaymentStatus, Character> {

    @Override
    public Character convert(final @NotNull PaymentStatus source) {
        return source.getValue();
    }
}
