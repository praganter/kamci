package dev.batuhanyetgin.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "dev.batuhanyetgin")
@EnableDiscoveryClient
public class AuthServiceApplication {

    public static void main(final String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}