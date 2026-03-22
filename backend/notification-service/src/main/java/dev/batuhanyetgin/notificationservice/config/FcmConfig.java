package dev.batuhanyetgin.notificationservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FcmConfig {

    @Bean
    public FirebaseApp firebaseApp() {
        InputStream credStream = null;

        String credPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (credPath != null && !credPath.isBlank()) {
            try {
                credStream = new FileInputStream(credPath);
            } catch (IOException e) {
                log.error("GOOGLE_APPLICATION_CREDENTIALS path not found: {}", credPath);
                return null;
            }
        } else {
            ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
            if (!resource.exists()) {
                log.warn("Firebase credentials not configured, FCM disabled");
                return null;
            }
            try {
                credStream = resource.getInputStream();
            } catch (IOException e) {
                log.error("Failed to read firebase-service-account.json: {}", e.getMessage(), e);
                return null;
            }
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        try {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credStream))
                    .build();
            log.info("Initializing FirebaseApp");
            return FirebaseApp.initializeApp(options);
        } catch (IOException e) {
            log.error("Failed to initialize FirebaseApp: {}", e.getMessage(), e);
            return null;
        }
    }
}