package dev.batuhanyetgin.kamci.mapper;

import dev.batuhanyetgin.commonmodule.mapper.BaseMapper;
import dev.batuhanyetgin.kamci.document.DebtDocument;
import dev.batuhanyetgin.kamci.dto.response.DebtItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface DebtDocumentToDebtItemResponseMapper extends BaseMapper<DebtDocument, DebtItemResponse> {

    @Mapping(target = "paidCurrentPeriod", ignore = true)
    DebtItemResponse map(DebtDocument debtDocument);
}