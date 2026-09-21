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
public class HouseholdInviteEvent extends BaseEvent {
    private String householdId;
    private String inviterUserId;
    private String inviterEmail;
    private String inviteeUserId;
    private String inviteeEmail;
}
