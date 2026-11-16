package dev.batuhanyetgin.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Native profile serves files from a plain folder (what compose mounts at /config). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.profiles.active=native",
        "eureka.client.enabled=false",
        "management.tracing.enabled=false"
})
class ConfigServerNativeTest {

    static {
        System.setProperty("CONFIG_NATIVE_LOCATIONS",
                "file:" + Path.of("src/test/resources/config").toAbsolutePath().toUri().getPath());
    }

    @LocalServerPort
    int port;

    private String get(String path) throws Exception {
        HttpResponse<String> r = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(r.statusCode()).isEqualTo(200);
        return r.body();
    }

    @Test
    void servesPropertiesFromNativeFolder() throws Exception {
        assertThat(get("/gateway-server/default")).contains("from-native-folder");
    }

    @Test
    void actuatorHealthIsUp() throws Exception {
        assertThat(get("/actuator/health")).contains("\"UP\"");
    }
}
