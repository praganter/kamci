package dev.batuhanyetgin.commonmodule.model;

import dev.batuhanyetgin.commonmodule.model.response.BaseResponse;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse extends BaseResponse {

    private Instant timestamp;
    private int status;
    private String code;
    private String message;
    private String path;
    private String traceId;

    private List<ValidationError> errors;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationError {
        private String field;
        private String message;
    }
}