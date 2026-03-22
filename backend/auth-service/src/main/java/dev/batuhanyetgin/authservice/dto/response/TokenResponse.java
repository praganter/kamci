package dev.batuhanyetgin.authservice.dto.response;

import dev.batuhanyetgin.commonmodule.model.response.BaseResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TokenResponse extends BaseResponse {
    private String token;
    private String refreshToken;
}