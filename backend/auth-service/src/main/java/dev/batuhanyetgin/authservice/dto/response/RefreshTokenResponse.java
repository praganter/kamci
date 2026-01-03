package dev.batuhanyetgin.authservice.dto.response;

import dev.batuhanyetgin.commonmodule.model.response.BaseResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class RefreshTokenResponse extends BaseResponse {
    private String token;
    private String refreshToken;
}