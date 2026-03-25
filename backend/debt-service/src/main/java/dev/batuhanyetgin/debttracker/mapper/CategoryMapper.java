package dev.batuhanyetgin.debttracker.mapper;

import dev.batuhanyetgin.debttracker.document.CategoryDocument;
import dev.batuhanyetgin.debttracker.dto.response.GetAllCategoryResponse;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public GetAllCategoryResponse convert(final CategoryDocument categoryDocument) {
        return Mappers.getMapper(CategoryDocumentToGetAllCategoryResponseMapper.class).map(categoryDocument);
    }
}
