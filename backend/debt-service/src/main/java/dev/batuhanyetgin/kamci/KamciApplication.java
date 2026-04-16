package dev.batuhanyetgin.kamci;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "dev.batuhanyetgin")
@EnableDiscoveryClient
@EnableScheduling
@EnableFeignClients
public class KamciApplication {

    public static void main(final String[] args) {
        SpringApplication.run(KamciApplication.class, args);
    }
}