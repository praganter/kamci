package dev.batuhanyetgin.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class VerifyReceiptRequest {

    @NotNull(message = "{error.validation.not_null}")
    @Pattern(regexp = "IOS|ANDROID", message = "{error.validation.subscription.platform}")
    private String platform;

    @NotNull(message = "{error.validation.not_null}")
    @Pattern(regexp = "MONTHLY|YEARLY", message = "{error.validation.subscription.plan}")
    private String plan;

    @NotBlank(message = "{error.validation.not_blank}")
    private String receiptData;

    @NotBlank(message = "{error.validation.not_blank}")
    private String productId;

    private String packageName;
}
