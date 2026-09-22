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
public class WeeklyDigestEvent extends BaseEvent {
    private String ownerId;
    private LocalDateTime weekStart;
    private LocalDateTime weekEnd;
    private BigDecimal totalAmount;
    private List<WeeklyDigestItem> items;
}
