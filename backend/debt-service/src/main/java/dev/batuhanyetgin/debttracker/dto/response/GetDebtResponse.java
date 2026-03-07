package dev.batuhanyetgin.debttracker.dto.response;

import dev.batuhanyetgin.debttracker.document.DebtDocument;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class GetDebtResponse {
    private List<DebtDocument> ownerDebts;
    private List<DebtDocument> sharedDebts;
}