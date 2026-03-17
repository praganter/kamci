package dev.batuhanyetgin.debttracker.mapper;

import dev.batuhanyetgin.commonmodule.mapper.BaseMapper;
import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.dto.request.UpdateDebtRequest;
import org.mapstruct.Mapper;

@Mapper
public interface UpdateDebtRequestToDebtDocumentMapper extends BaseMapper<UpdateDebtRequest, DebtDocument> {

    DebtDocument map(UpdateDebtRequest updateDebtRequest);
}