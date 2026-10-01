package dev.batuhanyetgin.kamci.client;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthClientFallbackFactoryTest {

    private final AuthClientFallbackFactory factory = new AuthClientFallbackFactory();
    private final AuthClient fallback = factory.create(new RuntimeException("auth-service down"));

    @Test
    @DisplayName("userExists → throws service-unavailable")
    void userExists_throwsServiceUnavailable() {
        assertThatThrownBy(() -> fallback.userExists("user1"))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getStatus())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("getUserIdByEmail → throws service-unavailable")
    void getUserIdByEmail_throwsServiceUnavailable() {
        assertThatThrownBy(() -> fallback.getUserIdByEmail("user@example.com"))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getStatus())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("getHouseholdMemberIds → degrades to personal-only list, does not throw")
    void getHouseholdMemberIds_degradesToPersonalOnly() {
        final List<String> result = fallback.getHouseholdMemberIds("user1");

        assertThat(result).containsExactly("user1");
    }
}
