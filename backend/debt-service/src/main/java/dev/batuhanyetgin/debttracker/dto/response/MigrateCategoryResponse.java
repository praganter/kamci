package dev.batuhanyetgin.debttracker.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MigrateCategoryResponse {
    private int migratedDebtCount;
}
