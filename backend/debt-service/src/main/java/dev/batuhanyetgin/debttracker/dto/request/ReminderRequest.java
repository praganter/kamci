package dev.batuhanyetgin.debttracker.dto.request;

import dev.batuhanyetgin.commonmodule.event.ReminderChannelType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class ReminderRequest {

    private boolean enabled;

    @NotEmpty(message = "{error.validation.not_blank}")
    private List<ReminderChannelType> channels;

    @NotNull(message = "{error.validation.not_null}")
    private LocalDate reminderDate;

    @NotNull(message = "{error.validation.not_null}")
    private LocalTime reminderTime;
}