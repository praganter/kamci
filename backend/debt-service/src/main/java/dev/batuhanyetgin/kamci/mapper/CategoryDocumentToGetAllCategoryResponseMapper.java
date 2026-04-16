package dev.batuhanyetgin.kamci.mapper;

import dev.batuhanyetgin.commonmodule.mapper.BaseMapper;
import dev.batuhanyetgin.kamci.document.CategoryDocument;
import dev.batuhanyetgin.kamci.dto.response.GetAllCategoryResponse;
import org.mapstruct.Mapper;

@Mapper
public interface CategoryDocumentToGetAllCategoryResponseMapper extends BaseMapper<CategoryDocument, GetAllCategoryResponse> {

    GetAllCategoryResponse map(CategoryDocument categoryDocument);
}
