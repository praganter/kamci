package dev.batuhanyetgin.debttracker.mapper;

import dev.batuhanyetgin.commonmodule.mapper.BaseMapper;
import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.dto.response.DebtDetailResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface DebtDocumentToDebtDetailResponseMapper extends BaseMapper<DebtDocument, DebtDetailResponse> {

    @Mapping(target = "paidInstallmentCount",
            expression = "java(debtDocument.getPayments() != null ? debtDocument.getPayments().size() : 0)")
    DebtDetailResponse map(DebtDocument debtDocument);
}
