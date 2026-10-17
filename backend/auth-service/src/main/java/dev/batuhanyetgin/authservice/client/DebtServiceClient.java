package dev.batuhanyetgin.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "debt-service", fallbackFactory = DebtServiceClientFallbackFactory.class)
public interface DebtServiceClient {

    @DeleteMapping("debt/internal/account-data/{userId}")
    void deleteAccountData(@PathVariable String userId);
}
