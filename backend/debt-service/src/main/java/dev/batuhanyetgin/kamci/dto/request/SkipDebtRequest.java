package dev.batuhanyetgin.kamci.dto.request;

import jakarta.validation.constraints.PastOrPresent;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkipDebtRequest {

    @PastOrPresent(message = "{error.validation.past_or_present}")
    private LocalDateTime skippedAt;
}