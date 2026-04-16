package dev.batuhanyetgin.kamci.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetPaidInstallmentsRequest {

    @NotNull
    @Min(value = 1, message = "{error.validation.min}")
    private Integer count;
}