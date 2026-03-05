package dev.batuhanyetgin.debttracker.converter.write;

import dev.batuhanyetgin.debttracker.enums.DebtStatus;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.stereotype.Component;

@Component
@WritingConverter
public class DebtStatusWriteConverter implements Converter<DebtStatus, Character> {

    @Override
    public Character convert(@NotNull DebtStatus source) {
        return source.getValue();
    }
}