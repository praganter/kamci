package dev.batuhanyetgin.debttracker.mapper;

import dev.batuhanyetgin.commonmodule.mapper.BaseMapper;
import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.dto.response.DebtItemResponse;
import org.mapstruct.Mapper;

@Mapper
public interface DebtDocumentToDebtItemResponseMapper extends BaseMapper<DebtDocument, DebtItemResponse> {

    DebtItemResponse map(DebtDocument debtDocument);
}