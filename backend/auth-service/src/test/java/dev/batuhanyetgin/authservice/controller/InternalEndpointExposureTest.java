package dev.batuhanyetgin.authservice.controller;

import dev.batuhanyetgin.authservice.document.AuthDocument;
import dev.batuhanyetgin.authservice.repositoryservice.AuthRepositoryService;
import dev.batuhanyetgin.authservice.service.AuthService;
import dev.batuhanyetgin.authservice.service.HouseholdService;
import dev.batuhanyetgin.authservice.service.SubscriptionService;
import dev.batuhanyetgin.commonmodule.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real routing + HTTP-layer checks (MockMvc) for the security properties the gateway relies on:
 * inter-service lookups live under {@code auth/internal/} only, and the RevenueCat webhook
 * rejects everything without the shared secret.
 */
@ExtendWith(MockitoExtension.class)
class InternalEndpointExposureTest {

    private static final String WEBHOOK = "/subscription/revenuecat/webhook";
    private static final String SECRET = "rc-secret";
    private static final String PAYLOAD = "{\"event\":{\"type\":\"RENEWAL\",\"app_user_id\":\"u1\"}}";

    @Mock private AuthService authService;
    @Mock private AuthRepositoryService authRS;
    @Mock private HouseholdService householdService;
    @Mock private SubscriptionService subscriptionService;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        final StaticMessageSource messages = new StaticMessageSource();
        messages.setUseCodeAsDefaultMessage(true);

        final SubscriptionController subscription = new SubscriptionController(subscriptionService);
        ReflectionTestUtils.setField(subscription, "adminSecretKey", "admin");
        ReflectionTestUtils.setField(subscription, "revenueCatWebhookSecret", SECRET);

        mvc = MockMvcBuilders.standaloneSetup(
                        new AuthController(authService, authRS),
                        new InternalHouseholdController(householdService),
                        subscription)
                .setControllerAdvice(new GlobalExceptionHandler(messages))
                .build();
    }

    // ─── user lookups only under auth/internal/ ───────────────────────────────

    @Test
    @DisplayName("user-exists is served under auth/internal/ and not under the old public path")
    void userExists_onlyInternal() throws Exception {
        when(authRS.existsById("u1")).thenReturn(true);

        mvc.perform(get("/auth/internal/user/exists/u1")).andExpect(status().isOk()).andExpect(content().string("true"));
        mvc.perform(get("/auth/user/exists/u1")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("id-by-email is served under auth/internal/ and not under the old public path")
    void idByEmail_onlyInternal() throws Exception {
        final AuthDocument user = new AuthDocument();
        user.setId("u1");
        when(authRS.findByEMail("a@b.co")).thenReturn(user);

        mvc.perform(get("/auth/internal/user/id-by-email/a@b.co")).andExpect(status().isOk());
        mvc.perform(get("/auth/user/id-by-email/a@b.co")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("household member lookup is served under auth/internal/ and not under auth/household/")
    void householdMembers_onlyInternal() throws Exception {
        when(householdService.getMemberIds("u1")).thenReturn(List.of("u1", "u2"));

        mvc.perform(get("/auth/internal/household/members/u1")).andExpect(status().isOk());
        mvc.perform(get("/auth/household/members/u1")).andExpect(status().isNotFound());
    }

    // ─── RevenueCat webhook authentication ────────────────────────────────────

    @Test
    @DisplayName("webhook without Authorization header → 403, event not processed")
    void webhook_noHeader_forbidden() throws Exception {
        mvc.perform(post(WEBHOOK).contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isForbidden());
        verify(subscriptionService, never()).handleRevenueCatWebhook(any());
    }

    @Test
    @DisplayName("webhook with wrong secret → 403, event not processed")
    void webhook_wrongSecret_forbidden() throws Exception {
        mvc.perform(post(WEBHOOK).header("Authorization", "Bearer nope")
                        .contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isForbidden());
        verify(subscriptionService, never()).handleRevenueCatWebhook(any());
    }

    @Test
    @DisplayName("webhook with the shared secret → 200 and the JSON body reaches the service")
    void webhook_validSecret_processed() throws Exception {
        mvc.perform(post(WEBHOOK).header("Authorization", "Bearer " + SECRET)
                        .contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isOk());
        verify(subscriptionService).handleRevenueCatWebhook(any());
    }

    @Test
    @DisplayName("webhook secret left unconfigured → 403 even for 'Bearer ' (no open default)")
    void webhook_unconfiguredSecret_forbidden() throws Exception {
        final SubscriptionController open = new SubscriptionController(subscriptionService);
        ReflectionTestUtils.setField(open, "revenueCatWebhookSecret", "");
        final MockMvc unconfigured = MockMvcBuilders.standaloneSetup(open)
                .setControllerAdvice(new GlobalExceptionHandler(new StaticMessageSource()))
                .build();

        unconfigured.perform(post(WEBHOOK).header("Authorization", "Bearer ")
                        .contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isForbidden());
    }
}
