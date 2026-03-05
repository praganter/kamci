package dev.batuhanyetgin.debttracker.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DeleteDebtResponse {
    private String message;
}