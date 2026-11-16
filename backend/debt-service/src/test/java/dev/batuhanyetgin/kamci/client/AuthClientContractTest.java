package dev.batuhanyetgin.kamci.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class AuthClientContractTest {

    @Test
    @DisplayName("every auth-service call made by debt-service targets auth/internal/ (blocked at the gateway)")
    void allCallsUseInternalPath() {
        assertThat(AuthClient.class.getDeclaredMethods())
                .isNotEmpty()
                .allSatisfy(m -> assertThat(Arrays.asList(m.getAnnotation(GetMapping.class).value()))
                        .allMatch(path -> path.startsWith("auth/internal/")));
    }
}
