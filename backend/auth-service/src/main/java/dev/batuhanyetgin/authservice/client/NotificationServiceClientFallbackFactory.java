package dev.batuhanyetgin.authservice.client;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationServiceClientFallbackFactory implements FallbackFactory<NotificationServiceClient> {

    @Override
    public NotificationServiceClient create(final Throwable cause) {
        return userId -> {
            log.error("notification-service unavailable for deleteAccountData(userId={}): {}", userId, cause.getMessage());
            throw BaseException.serviceUnavailable("error.notification_service.unavailable");
        };
    }
}
