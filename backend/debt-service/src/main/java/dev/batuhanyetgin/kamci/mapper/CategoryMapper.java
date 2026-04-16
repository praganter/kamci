package dev.batuhanyetgin.kamci.mapper;

import dev.batuhanyetgin.kamci.document.CategoryDocument;
import dev.batuhanyetgin.kamci.dto.response.GetAllCategoryResponse;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public GetAllCategoryResponse convert(final CategoryDocument categoryDocument) {
        return Mappers.getMapper(CategoryDocumentToGetAllCategoryResponseMapper.class).map(categoryDocument);
    }
}
