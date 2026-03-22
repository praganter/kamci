package dev.batuhanyetgin.debttracker.mapper;

import dev.batuhanyetgin.commonmodule.mapper.BaseMapper;
import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.dto.response.DebtDetailResponse;
import org.mapstruct.Mapper;

@Mapper
public interface DebtDocumentToDebtDetailResponseMapper extends BaseMapper<DebtDocument, DebtDetailResponse> {

    DebtDetailResponse map(DebtDocument debtDocument);
}
