package dev.batuhanyetgin.debttracker.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class CategoryDebtResponse {

    private String categoryId;

    private String categoryName;

    private boolean owned;

    private String ownerUserId;

    private List<DebtItemResponse> debts;
}