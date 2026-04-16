package dev.batuhanyetgin.kamci.mapper;

import dev.batuhanyetgin.commonmodule.mapper.BaseMapper;
import dev.batuhanyetgin.kamci.document.DebtDocument;
import dev.batuhanyetgin.kamci.dto.request.UpdateDebtRequest;
import org.mapstruct.Mapper;

@Mapper
public interface UpdateDebtRequestToDebtDocumentMapper extends BaseMapper<UpdateDebtRequest, DebtDocument> {

    DebtDocument map(UpdateDebtRequest updateDebtRequest);
}