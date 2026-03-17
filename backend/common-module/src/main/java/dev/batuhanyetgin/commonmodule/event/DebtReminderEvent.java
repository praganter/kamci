package dev.batuhanyetgin.commonmodule.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DebtReminderEvent extends BaseEvent {
    private String debtId;
    private String ownerId;
    private String debtName;
    private BigDecimal amount;
    private LocalDateTime nextReminderDate;
    private List<ReminderChannelType> channels;
}