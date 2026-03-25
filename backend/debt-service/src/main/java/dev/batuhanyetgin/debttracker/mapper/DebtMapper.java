package dev.batuhanyetgin.debttracker.mapper;

import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.dto.request.CreateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.request.UpdateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.response.DebtDetailResponse;
import dev.batuhanyetgin.debttracker.dto.response.DebtItemResponse;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;

@Component
public class DebtMapper {

    public DebtDocument convert(final CreateDebtRequest createDebtRequest) {
        return Mappers.getMapper(CreateDebtRequestToDebtDocumentMapper.class).map(createDebtRequest);
    }

    public DebtDocument convert(final UpdateDebtRequest updateDebtRequest) {
        return Mappers.getMapper(UpdateDebtRequestToDebtDocumentMapper.class).map(updateDebtRequest);
    }

    public DebtItemResponse convert(final DebtDocument debtDocument) {
        return Mappers.getMapper(DebtDocumentToDebtItemResponseMapper.class).map(debtDocument);
    }

    public DebtDetailResponse convertToDetail(final DebtDocument debtDocument) {
        return Mappers.getMapper(DebtDocumentToDebtDetailResponseMapper.class).map(debtDocument);
    }
}