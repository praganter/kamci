package dev.batuhanyetgin.authservice.dto.response;

import dev.batuhanyetgin.commonmodule.model.response.BaseResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class UserDetailResponse extends BaseResponse {
    private String email;
    private boolean premium;
    private LocalDateTime createdAt;
}
