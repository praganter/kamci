package dev.batuhanyetgin.kamci.converter.write;

import dev.batuhanyetgin.kamci.enums.DebtStatus;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.stereotype.Component;

@Component
@WritingConverter
public class DebtStatusWriteConverter implements Converter<DebtStatus, Character> {

    @Override
    public Character convert(final @NotNull DebtStatus source) {
        return source.getValue();
    }
}