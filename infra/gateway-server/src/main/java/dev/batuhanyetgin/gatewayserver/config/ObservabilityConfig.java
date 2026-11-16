package dev.batuhanyetgin.gatewayserver.config;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Bean
    public ObservationPredicate noActuatorObservations() {
        return (name, context) -> {
            if ("http.server.requests".equals(name)) {
                String contextualName = context.getContextualName();
                return contextualName == null || !contextualName.contains("/actuator");
            }
            return true;
        };
    }
}