package dev.batuhanyetgin.commonmodule.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DebtAddedEvent extends BaseEvent {
    private String debtId;
    private String ownerId;
    private String debtName;
    private BigDecimal amount;
    private List<String> sharedWithUserIds;
}