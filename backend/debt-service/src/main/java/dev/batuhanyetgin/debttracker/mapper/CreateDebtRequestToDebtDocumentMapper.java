package dev.batuhanyetgin.debttracker.mapper;

import dev.batuhanyetgin.commonmodule.mapper.BaseMapper;
import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.dto.request.CreateDebtRequest;
import org.mapstruct.Mapper;

@Mapper
public interface CreateDebtRequestToDebtDocumentMapper extends BaseMapper<CreateDebtRequest, DebtDocument> {

    DebtDocument map(CreateDebtRequest createDebtRequest);
}