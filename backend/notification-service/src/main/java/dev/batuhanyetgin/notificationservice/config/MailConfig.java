package dev.batuhanyetgin.notificationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;

@Configuration
public class MailConfig {

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Bean
    public SimpleMailMessage templateMessage() {
        final SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("Debt Tracker <" + fromAddress + ">");
        return message;
    }
}
