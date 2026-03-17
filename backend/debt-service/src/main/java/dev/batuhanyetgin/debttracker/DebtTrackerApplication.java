package dev.batuhanyetgin.debttracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "dev.batuhanyetgin")
@EnableDiscoveryClient
@EnableScheduling
public class DebtTrackerApplication {

    public static void main(final String[] args) {
        SpringApplication.run(DebtTrackerApplication.class, args);
    }
}