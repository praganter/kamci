package dev.batuhanyetgin.gatewayserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import static dev.batuhanyetgin.gatewayserver.GatewayTestSupport.SECRET;
import static dev.batuhanyetgin.gatewayserver.GatewayTestSupport.accessToken;
import static org.assertj.core.api.Assertions.assertThat;

/** Uses only the routes from the real {@code dt-config/gateway-server.yml} (no test route). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.import=classpath:/dt-config/gateway-server.yml",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "management.tracing.enabled=false",
        "JWT_SECRET=" + SECRET
})
@AutoConfigureWebTestClient
class GatewayRoutingConfigTest {

    @Autowired
    ApplicationContext ctx;
    @Autowired
    WebTestClient client;

    @Autowired
    RouteLocator routeLocator;

    @Test
    void configuredRoutesAreLoaded() {
        // Guards against a silently ignored property prefix (Cloud Gateway 5: spring.cloud.gateway.server.webflux.*).
        assertThat(routeLocator.getRoutes().map(Route::getId).collectList().block())
                .containsExactlyInAnyOrder("debt-service", "cache-service", "auth-service", "notification-service");
    }

    @Test
    void routedPaths_passThroughTheAuthFilter() {
        for (String path : new String[]{"/debt/get", "/category/x", "/income/x", "/rates/x", "/auth/me", "/notification/x"}) {
            client.get().uri(path).exchange().expectStatus().isUnauthorized();
        }
    }

    @Test
    void discoveryLocator_isDisabled() {
        assertThat(ctx.getBeansOfType(DiscoveryClientRouteDefinitionLocator.class)).isEmpty();
    }

    @Test
    void serviceIdPaths_areNotRoutable() {
        String auth = "Bearer " + accessToken("u", true);
        for (String path : new String[]{"/debt-service/debt/get", "/DEBT-SERVICE/debt/get", "/auth-service/auth/me",
                "/eureka-server/eureka/apps"}) {
            client.get().uri(path).header(HttpHeaders.AUTHORIZATION, auth).exchange().expectStatus().isNotFound();
        }
    }
}
