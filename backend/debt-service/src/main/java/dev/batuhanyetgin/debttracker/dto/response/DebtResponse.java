package dev.batuhanyetgin.debttracker.dto.response;

import dev.batuhanyetgin.debttracker.document.DebtDocument;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateDebtResponse {
    private DebtDocument debt;
}