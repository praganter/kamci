package dev.batuhanyetgin.kamci.document.embedded;

import dev.batuhanyetgin.commonmodule.event.ReminderChannelType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderEmbedded {

    @Builder.Default
    private boolean enabled = false;

    private List<ReminderChannelType> channels;

    private LocalDate reminderDate;

    private LocalTime reminderTime;
}