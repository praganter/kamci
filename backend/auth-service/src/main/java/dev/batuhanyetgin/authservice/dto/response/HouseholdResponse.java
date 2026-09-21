package dev.batuhanyetgin.authservice.dto.response;

import dev.batuhanyetgin.authservice.enums.HouseholdStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class HouseholdResponse {

    private String id;

    private String ownerId;

    private String ownerEmail;

    private String partnerId;

    private String partnerEmail;

    private HouseholdStatus status;

    private boolean owner;
}
