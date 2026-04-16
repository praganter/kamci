package dev.batuhanyetgin.kamci.converter.read;

import com.mongodb.lang.NonNull;
import dev.batuhanyetgin.kamci.enums.AccessLevelType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;

@Component
@ReadingConverter
public class AccessLevelTypeReadConverter implements Converter<Character, AccessLevelType> {

    @Override
    public AccessLevelType convert(final @NonNull Character source) {
        for (AccessLevelType type : AccessLevelType.values()) {
            if (type.getValue().equals(source)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown AccessLevelType: " + source);
    }
}