package dev.batuhanyetgin.gatewayserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
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
