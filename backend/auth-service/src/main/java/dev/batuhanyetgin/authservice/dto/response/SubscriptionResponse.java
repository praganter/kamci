package dev.batuhanyetgin.authservice.dto.response;

import dev.batuhanyetgin.authservice.enums.SubscriptionPlan;
import dev.batuhanyetgin.authservice.enums.SubscriptionStatus;
import dev.batuhanyetgin.commonmodule.model.response.BaseResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class SubscriptionResponse extends BaseResponse {
    private SubscriptionPlan plan;
    private SubscriptionStatus status;
    private LocalDateTime expiryDate;
    private String accessToken;
    private String refreshToken;
}
