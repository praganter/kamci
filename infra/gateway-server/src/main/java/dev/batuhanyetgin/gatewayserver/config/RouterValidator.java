package dev.batuhanyetgin.gatewayserver.config;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouterValidator {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final List<String> openApiEndpoints;

    public RouterValidator(EndpointProperties properties) {
        this.openApiEndpoints = properties.getPublicEndpoints();
    }

    public Predicate<ServerHttpRequest> isSecured() {
        return request -> {
            String path = request.getURI().getPath().toLowerCase();
            return openApiEndpoints.stream()
                    .noneMatch(uri -> {
                        String pattern = uri.trim().toLowerCase();
                        return path.equals(pattern)
                                || pathMatcher.match(pattern, path)
                                || path.equals(pattern + "/");
                    });
        };
    }
}