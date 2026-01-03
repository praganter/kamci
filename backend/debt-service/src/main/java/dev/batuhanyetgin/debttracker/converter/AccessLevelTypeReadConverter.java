package dev.batuhanyetgin.debttracker.converter;

import com.mongodb.lang.NonNull;
import dev.batuhanyetgin.debttracker.enums.AccessLevelType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;

@Component
@ReadingConverter
public class AccessLevelTypeReadConverter implements Converter<Character, AccessLevelType> {

    @Override
    public AccessLevelType convert(@NonNull Character source) {
        for (AccessLevelType type : AccessLevelType.values()) {
            if (type.getValue().equals(source)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown AccessLevelType: " + source);
    }

}