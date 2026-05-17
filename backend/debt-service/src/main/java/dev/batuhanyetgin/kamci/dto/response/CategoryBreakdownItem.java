package dev.batuhanyetgin.kamci.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class CategoryBreakdownItem {

    private String categoryId;

    private String categoryName;

    private BigDecimal totalPaid;

    private int debtCount;

    private BigDecimal percentage;
}
