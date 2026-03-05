package dev.batuhanyetgin.debttracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "dev.batuhanyetgin")
@EnableDiscoveryClient
public class DebtTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DebtTrackerApplication.class, args);
    }
}