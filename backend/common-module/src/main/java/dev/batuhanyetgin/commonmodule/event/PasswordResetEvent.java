package dev.batuhanyetgin.commonmodule.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PasswordResetEvent extends BaseEvent {
    private String userId;
    private String email;
    private String resetToken;
}