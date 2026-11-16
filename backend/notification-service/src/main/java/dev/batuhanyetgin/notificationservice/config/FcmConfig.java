package dev.batuhanyetgin.notificationservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

// `local` profile: no FirebaseApp bean at all, FcmService then logs and skips every push.
@Profile("!local")
@Slf4j
@Configuration
public class FcmConfig {

    @Bean
    public FirebaseApp firebaseApp() {
        log.info("Starting FirebaseApp initialization...");
        InputStream credStream = null;

        String credPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (credPath != null && !credPath.isBlank()) {
            log.info("Using credentials from environment: {}", credPath);
            try {
                credStream = new FileInputStream(credPath);
            } catch (IOException e) {
                log.error("GOOGLE_APPLICATION_CREDENTIALS path not found: {}", credPath);
                return null;
            }
        } else {
            ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
            log.info("Attempting to load firebase-service-account.json from classpath");
            if (!resource.exists()) {
                log.warn("Firebase credentials file NOT FOUND in classpath, FCM disabled");
                return null;
            }
            try {
                credStream = resource.getInputStream();
                log.info("Successfully opened firebase-service-account.json input stream");
            } catch (IOException e) {
                log.error("Failed to read firebase-service-account.json: {}", e.getMessage(), e);
                return null;
            }
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("FirebaseApp already initialized, returning existing instance");
            return FirebaseApp.getInstance();
        }
        try {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credStream))
                    .build();
            log.info("Initializing FirebaseApp with project settings");
            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("FirebaseApp successfully initialized: {}", app.getName());
            return app;
        } catch (IOException e) {
            log.error("Failed to initialize FirebaseApp: {}", e.getMessage(), e);
            return null;
        }
    }
}