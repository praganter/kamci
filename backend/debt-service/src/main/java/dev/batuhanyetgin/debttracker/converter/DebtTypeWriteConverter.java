package dev.batuhanyetgin.debttracker.converter;

import dev.batuhanyetgin.debttracker.enums.DebtType;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.stereotype.Component;

@Component
@WritingConverter
public class DebtTypeWriteConverter implements Converter<DebtType, Character> {

    @Override
    public Character convert(@NotNull DebtType source) {
        return source.getValue();
    }
}