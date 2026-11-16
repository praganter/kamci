package dev.batuhanyetgin.gatewayserver.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "endpoints")
@Getter
@Setter
public class EndpointProperties {
    private List<String> publicEndpoints;
}