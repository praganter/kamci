package dev.batuhanyetgin.debttracker.mapper;

import dev.batuhanyetgin.commonmodule.mapper.BaseMapper;
import dev.batuhanyetgin.debttracker.document.CategoryDocument;
import dev.batuhanyetgin.debttracker.dto.response.GetAllCategoryResponse;
import org.mapstruct.Mapper;

@Mapper
public interface CategoryDocumentToGetAllCategoryResponseMapper extends BaseMapper<CategoryDocument, GetAllCategoryResponse> {

    GetAllCategoryResponse map(CategoryDocument categoryDocument);
}
