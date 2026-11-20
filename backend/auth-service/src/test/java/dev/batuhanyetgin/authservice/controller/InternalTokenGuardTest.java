package dev.batuhanyetgin.authservice.controller;

import dev.batuhanyetgin.authservice.repositoryservice.AuthRepositoryService;
import dev.batuhanyetgin.authservice.service.AuthService;
import dev.batuhanyetgin.authservice.service.HouseholdService;
import dev.batuhanyetgin.commonmodule.config.InternalApiConfig;
import dev.batuhanyetgin.commonmodule.config.InternalApiConfig.InternalTokenInterceptor;
import dev.batuhanyetgin.commonmodule.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalTokenGuardTest {

    private static final String TOKEN = "svc-token";

    @Mock private AuthService authService;
    @Mock private AuthRepositoryService authRS;
    @Mock private HouseholdService householdService;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        final StaticMessageSource messages = new StaticMessageSource();
        messages.setUseCodeAsDefaultMessage(true);
        mvc = MockMvcBuilders.standaloneSetup(
                        new AuthController(authService, authRS), new InternalHouseholdController(householdService))
                .addInterceptors(new InternalTokenInterceptor(TOKEN))
                .setControllerAdvice(new GlobalExceptionHandler(messages))
                .build();
    }

    @Test
    @DisplayName("user-exists without token -> 403, repository untouched")
    void userExists_noToken() throws Exception {
        mvc.perform(get("/auth/internal/user/exists/u1")).andExpect(status().isForbidden());
        verify(authRS, never()).existsById("u1");
    }

    @Test
    @DisplayName("id-by-email with wrong token -> 403")
    void idByEmail_wrongToken() throws Exception {
        mvc.perform(get("/auth/internal/user/id-by-email/a@b.co").header(InternalApiConfig.HEADER, "nope"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("household members without token -> 403, service untouched")
    void householdMembers_noToken() throws Exception {
        mvc.perform(get("/auth/internal/household/members/u1")).andExpect(status().isForbidden());
        verify(householdService, never()).getMemberIds("u1");
    }

    @Test
    @DisplayName("internal endpoints with the right token -> 200")
    void rightToken_ok() throws Exception {
        when(authRS.existsById("u1")).thenReturn(true);
        when(householdService.getMemberIds("u1")).thenReturn(List.of("u1"));

        mvc.perform(get("/auth/internal/user/exists/u1").header(InternalApiConfig.HEADER, TOKEN))
                .andExpect(status().isOk());
        mvc.perform(get("/auth/internal/household/members/u1").header(InternalApiConfig.HEADER, TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("public endpoints are not affected")
    void publicEndpoint_noTokenNeeded() throws Exception {
        mvc.perform(get("/auth/user-detail")).andExpect(status().isOk());
    }
}
