package dev.batuhanyetgin.kamci.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "auth-service", fallbackFactory = AuthClientFallbackFactory.class)
public interface AuthClient {

    @GetMapping("auth/user/exists/{userId}")
    boolean userExists(@PathVariable String userId);

    @GetMapping("auth/user/id-by-email/{email}")
    String getUserIdByEmail(@PathVariable String email);

    @GetMapping("auth/household/members/{userId}")
    List<String> getHouseholdMemberIds(@PathVariable String userId);
}