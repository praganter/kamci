package dev.batuhanyetgin.kamci.mapper;

import dev.batuhanyetgin.commonmodule.mapper.BaseMapper;
import dev.batuhanyetgin.kamci.document.DebtDocument;
import dev.batuhanyetgin.kamci.dto.request.CreateDebtRequest;
import org.mapstruct.Mapper;

@Mapper
public interface CreateDebtRequestToDebtDocumentMapper extends BaseMapper<CreateDebtRequest, DebtDocument> {

    DebtDocument map(CreateDebtRequest createDebtRequest);
}