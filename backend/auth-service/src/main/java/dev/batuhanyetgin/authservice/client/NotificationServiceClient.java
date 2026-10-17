package dev.batuhanyetgin.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "notification-service", fallbackFactory = NotificationServiceClientFallbackFactory.class)
public interface NotificationServiceClient {

    @DeleteMapping("notification/internal/account-data/{userId}")
    void deleteAccountData(@PathVariable String userId);
}
