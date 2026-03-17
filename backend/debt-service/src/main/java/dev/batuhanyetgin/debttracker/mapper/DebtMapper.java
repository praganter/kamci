package dev.batuhanyetgin.debttracker.mapper;

import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.dto.request.CreateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.request.UpdateDebtRequest;
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
}