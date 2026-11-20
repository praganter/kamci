package dev.batuhanyetgin.kamci.controller;

import dev.batuhanyetgin.commonmodule.config.InternalApiConfig;
import dev.batuhanyetgin.commonmodule.config.InternalApiConfig.InternalTokenInterceptor;
import dev.batuhanyetgin.commonmodule.exception.GlobalExceptionHandler;
import dev.batuhanyetgin.kamci.service.AccountDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalTokenGuardTest {

    private static final String PATH = "/debt/internal/account-data/u1";

    @Mock private AccountDataService accountDataService;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        final StaticMessageSource messages = new StaticMessageSource();
        messages.setUseCodeAsDefaultMessage(true);
        mvc = MockMvcBuilders.standaloneSetup(new InternalAccountController(accountDataService))
                .addInterceptors(new InternalTokenInterceptor("svc-token"))
                .setControllerAdvice(new GlobalExceptionHandler(messages))
                .build();
    }

    @Test
    @DisplayName("account-data delete without token -> 403, nothing deleted")
    void noToken_forbidden() throws Exception {
        mvc.perform(delete(PATH)).andExpect(status().isForbidden());
        verify(accountDataService, never()).deleteUserData("u1");
    }

    @Test
    @DisplayName("account-data delete with wrong token -> 403, nothing deleted")
    void wrongToken_forbidden() throws Exception {
        mvc.perform(delete(PATH).header(InternalApiConfig.HEADER, "nope")).andExpect(status().isForbidden());
        verify(accountDataService, never()).deleteUserData("u1");
    }

    @Test
    @DisplayName("account-data delete with the right token -> 204 and data deleted")
    void rightToken_deletes() throws Exception {
        mvc.perform(delete(PATH).header(InternalApiConfig.HEADER, "svc-token")).andExpect(status().isNoContent());
        verify(accountDataService).deleteUserData("u1");
    }
}
