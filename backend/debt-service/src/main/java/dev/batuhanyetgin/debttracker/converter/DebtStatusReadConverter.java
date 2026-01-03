package dev.batuhanyetgin.debttracker.converter;

import com.mongodb.lang.NonNull;
import dev.batuhanyetgin.debttracker.enums.DebtStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;

@Component
@ReadingConverter
public class DebtStatusReadConverter implements Converter<Character, DebtStatus> {

    @Override
    public DebtStatus convert(@NonNull Character source) {
        for (DebtStatus status : DebtStatus.values()) {
            if (status.getValue().equals(source)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown DebtStatus: " + source);
    }
}