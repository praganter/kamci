package dev.batuhanyetgin.debttracker.converter;

import com.mongodb.lang.NonNull;
import dev.batuhanyetgin.debttracker.enums.DebtType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;

@Component
@ReadingConverter
public class DebtTypeReadConverter implements Converter<Character, DebtType> {

    @Override
    public DebtType convert(@NonNull Character source) {
        for (DebtType type : DebtType.values()) {
            if (type.getValue().equals(source)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown DebtType: " + source);
    }

}