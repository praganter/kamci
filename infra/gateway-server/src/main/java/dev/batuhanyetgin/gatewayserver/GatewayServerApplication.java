package dev.batuhanyetgin.gatewayserver;

import dev.batuhanyetgin.gatewayserver.config.EndpointProperties;
import dev.batuhanyetgin.gatewayserver.config.RateLimitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties({EndpointProperties.class, RateLimitProperties.class})
public class GatewayServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServerApplication.class, args);
    }
}