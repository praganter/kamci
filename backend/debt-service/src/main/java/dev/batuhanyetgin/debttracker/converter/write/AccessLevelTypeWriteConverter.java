package dev.batuhanyetgin.debttracker.converter.write;

import dev.batuhanyetgin.debttracker.enums.AccessLevelType;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.stereotype.Component;

@Component
@WritingConverter
public class AccessLevelTypeWriteConverter implements Converter<AccessLevelType, Character> {

    @Override
    public Character convert(final @NotNull AccessLevelType source) {
        return source.getValue();
    }
}