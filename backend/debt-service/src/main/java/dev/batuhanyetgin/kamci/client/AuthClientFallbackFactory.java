package dev.batuhanyetgin.kamci.client;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AuthClientFallbackFactory implements FallbackFactory<AuthClient> {

    @Override
    public AuthClient create(final Throwable cause) {
        return new AuthClient() {
            @Override
            public boolean userExists(final String userId) {
                log.error("auth-service unavailable for userExists(userId={}): {}", userId, cause.getMessage());
                throw BaseException.serviceUnavailable("error.auth_service.unavailable");
            }

            @Override
            public String getUserIdByEmail(final String email) {
                log.error("auth-service unavailable for getUserIdByEmail(email={}): {}", email, cause.getMessage());
                throw BaseException.serviceUnavailable("error.auth_service.unavailable");
            }

            @Override
            public List<String> getHouseholdMemberIds(final String userId) {
                log.warn("auth-service unavailable for getHouseholdMemberIds(userId={}), degrading to personal-only: {}",
                        userId, cause.getMessage());
                return List.of(userId);
            }
        };
    }
}
