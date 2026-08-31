package dev.batuhanyetgin.kamci.mapper;

import dev.batuhanyetgin.commonmodule.mapper.BaseMapper;
import dev.batuhanyetgin.kamci.document.CategoryDocument;
import dev.batuhanyetgin.kamci.dto.response.GetAllCategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface CategoryDocumentToGetAllCategoryResponseMapper extends BaseMapper<CategoryDocument, GetAllCategoryResponse> {

    @Mapping(target = "members", source = "members")
    GetAllCategoryResponse map(CategoryDocument categoryDocument);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "accessLevel", source = "accessLevel")
    GetAllCategoryResponse.MemberItem mapMember(dev.batuhanyetgin.kamci.document.embedded.MemberEmbedded memberEmbedded);
}
