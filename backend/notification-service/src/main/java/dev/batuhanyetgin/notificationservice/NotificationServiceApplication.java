package dev.batuhanyetgin.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "dev.batuhanyetgin")
public class NotificationServiceApplication {

    public static void main(final String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}