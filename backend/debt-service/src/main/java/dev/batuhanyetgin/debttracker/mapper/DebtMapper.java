package dev.batuhanyetgin.debttracker.mapper;

import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.dto.request.CreateDebtRequest;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;

@Component
public class DebtMapper {

    public DebtDocument convert(CreateDebtRequest createDebtRequest) {
        return Mappers.getMapper(DebtRequestToDebtDocumentMapper.class).map(createDebtRequest);
    }
}