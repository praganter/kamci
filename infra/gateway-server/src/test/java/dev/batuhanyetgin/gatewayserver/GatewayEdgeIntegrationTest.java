package dev.batuhanyetgin.gatewayserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

import static dev.batuhanyetgin.gatewayserver.GatewayTestSupport.SECRET;
import static dev.batuhanyetgin.gatewayserver.GatewayTestSupport.accessToken;
import static dev.batuhanyetgin.gatewayserver.GatewayTestSupport.token;

/**
 * Runs the real gateway filter chain with the real {@code dt-config/gateway-server.yml}
 * (public endpoints, rate limits). Every request that survives the filters is forwarded to a local
 * {@code /echo} endpoint that reports the identity headers the downstream service would see.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.import=classpath:/dt-config/gateway-server.yml",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "management.tracing.enabled=false",
        "JWT_SECRET=" + SECRET
})
@AutoConfigureWebTestClient
class GatewayEdgeIntegrationTest {

    @TestConfiguration
    @Import(Echo.class)
    static class EchoRoute {
        @Bean
        RouteLocator echoRoute(RouteLocatorBuilder b) {
            return b.routes().route("echo", r -> r.order(-100).path("/**").uri("forward:/echo")).build();
        }
    }

    @RestController
    static class Echo {
        @GetMapping("/echo")
        Map<String, String> echo(@RequestHeader(value = "user-id", required = false) String userId,
                                 @RequestHeader(value = "user-premium", required = false) String premium) {
            return Map.of("userId", String.valueOf(userId), "premium", String.valueOf(premium));
        }
    }

    @Autowired
    WebTestClient client;
    @LocalServerPort
    int port;

    private WebTestClient.ResponseSpec get(String path, String... headers) {
        WebTestClient.RequestHeadersSpec<?> req = client.get().uri(URI.create("http://localhost:" + port + path));
        for (int i = 0; i < headers.length; i += 2) {
            req = req.header(headers[i], headers[i + 1]);
        }
        return req.exchange();
    }

    private void assertError(WebTestClient.ResponseSpec spec, int status, String code) {
        spec.expectStatus().isEqualTo(status).expectBody().jsonPath("$.code").isEqualTo(code);
    }

    // ---- JWT verification -------------------------------------------------------------

    @Test
    void validAccessToken_isForwardedWithIdentityHeaders() {
        get("/debt/get", HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("user-42", true))
                .expectStatus().isOk()
                .expectBody().jsonPath("$.userId").isEqualTo("user-42").jsonPath("$.premium").isEqualTo("true");
    }

    @Test
    void missingAuthorizationHeader_is401() {
        assertError(get("/debt/get"), 401, "error.auth.header_missing");
    }

    @Test
    void malformedAuthorizationHeader_is401() {
        assertError(get("/debt/get", HttpHeaders.AUTHORIZATION, "Basic abc"), 401, "error.auth.header_invalid");
    }

    @Test
    void tokenSignedWithWrongKey_is401() {
        String forged = token("u", "ACCESS", true, 60_000, "another-secret-another-secret-another-secret-1");
        assertError(get("/debt/get", HttpHeaders.AUTHORIZATION, "Bearer " + forged), 401, "error.auth.token_invalid");
    }

    @Test
    void garbageToken_is401() {
        assertError(get("/debt/get", HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"), 401, "error.auth.token_invalid");
    }

    @Test
    void expiredToken_is401() {
        String expired = token("u", "ACCESS", false, -1_000, SECRET);
        assertError(get("/debt/get", HttpHeaders.AUTHORIZATION, "Bearer " + expired), 401, "error.auth.token_expired");
    }

    @Test
    void refreshToken_isRejected() {
        String refresh = token("u", "REFRESH", false, 60_000, SECRET);
        assertError(get("/debt/get", HttpHeaders.AUTHORIZATION, "Bearer " + refresh), 401, "error.auth.token_invalid");
    }

    // ---- identity header stripping ----------------------------------------------------

    @Test
    void spoofedIdentityHeaders_areOverwrittenOnProtectedRoutes() {
        get("/debt/get", HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("real-user", false),
                "user-id", "victim", "User-ID", "victim2", "user-premium", "true")
                .expectStatus().isOk()
                .expectBody().jsonPath("$.userId").isEqualTo("real-user").jsonPath("$.premium").isEqualTo("false");
    }

    @Test
    void spoofedIdentityHeaders_areDroppedOnPublicRoutes() {
        get("/auth/register", "user-id", "victim", "user-premium", "true")
                .expectStatus().isOk()
                .expectBody().jsonPath("$.userId").isEqualTo("null").jsonPath("$.premium").isEqualTo("null");
    }

    // ---- internal path block ----------------------------------------------------------

    @Test
    void internalPaths_areNotFound_evenWithValidToken() {
        String auth = "Bearer " + accessToken("u", true);
        for (String path : new String[]{
                "/debt/internal/account-data/u",
                "/notification/internal/account-data/u",
                "/auth/internal/user/exists/u",
                "/auth/internal/household/members/u",
                "/auth/INTERNAL/household/members/u",
                "/auth/%69nternal/household/members/u",
                "/auth/%49NTERNAL/household/members/u",
                "/auth/internal"}) {
            try {
                assertError(get(path, HttpHeaders.AUTHORIZATION, auth), 404, "error.route.not_found");
            } catch (AssertionError e) {
                throw new AssertionError("path " + path + ": " + e.getMessage(), e);
            }
        }
    }

    @Test
    void matrixParamVariant_neverReachesDownstream() {
        // Rejected by the HTTP layer itself (400) before any filter; either way it is not forwarded.
        for (String path : new String[]{"/auth/internal;x=1/household/members/u", "/auth//internal/household/members/u"}) {
            get(path, HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("u", true)).expectStatus().is4xxClientError();
        }
    }

    @Test
    void internalLookalike_isNotBlocked() {
        get("/debt/internals", HttpHeaders.AUTHORIZATION, "Bearer " + accessToken("u", false)).expectStatus().isOk();
    }

    // ---- public endpoint list ---------------------------------------------------------

    @Test
    void publicEndpoints_needNoToken() {
        for (String path : new String[]{"/auth/logout", "/auth/register",
                "/subscription/revenuecat/webhook"}) {
            get(path).expectStatus().isOk();
        }
    }

    @Test
    void swaggerApiDocsAndEureka_areNotPublic() {
        for (String path : new String[]{"/swagger-ui.html", "/swagger-ui/index.html", "/v3/api-docs",
                "/auth-service/v3/api-docs", "/debt/v3/api-docs", "/api-docs", "/webjars/swagger-ui/index.html",
                "/eureka/apps", "/auth/me"}) {
            assertError(get(path), 401, "error.auth.header_missing");
        }
    }

    // ---- rate limiting ----------------------------------------------------------------

    @Test
    void forgetPassword_isRateLimited_afterCapacity() {
        for (int i = 0; i < 5; i++) {
            get("/auth/forget-password").expectStatus().isOk();
        }
        assertError(get("/auth/forget-password"), 429, "error.ratelimit.exceeded");
    }

    @Test
    void rateLimit_isPerPath() {
        for (int i = 0; i < 5; i++) {
            get("/auth/reset-password").expectStatus().isOk();
        }
        assertError(get("/auth/reset-password"), 429, "error.ratelimit.exceeded");
        get("/auth/refresh-token").expectStatus().isOk();
    }

    @Test
    void spoofedForwardedFor_doesNotBypassRateLimit() {
        for (int i = 0; i < 10; i++) {
            get("/auth/login", "X-Forwarded-For", "203.0.113." + i).expectStatus().isOk();
        }
        assertError(get("/auth/login", "X-Forwarded-For", "203.0.113.200"), 429, "error.ratelimit.exceeded");
    }
}
