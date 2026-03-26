package dev.batuhanyetgin.debttracker.mapper;

import dev.batuhanyetgin.commonmodule.mapper.BaseMapper;
import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.dto.response.DebtItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface DebtDocumentToDebtItemResponseMapper extends BaseMapper<DebtDocument, DebtItemResponse> {

    @Mapping(target = "paidCurrentPeriod", ignore = true)
    DebtItemResponse map(DebtDocument debtDocument);
}