package dev.batuhanyetgin.debttracker.converter.write;

import dev.batuhanyetgin.debttracker.enums.PaymentPeriodType;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.stereotype.Component;

@Component
@WritingConverter
public class PaymentPeriodTypeWriteConverter implements Converter<PaymentPeriodType, Character> {

    @Override
    public Character convert(@NotNull PaymentPeriodType source) {
        return source.getValue();
    }
}