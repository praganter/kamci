package dev.batuhanyetgin.notificationservice.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FcmService {

    private final FirebaseApp firebaseApp;

    public FcmService(@Nullable @Autowired(required = false) final FirebaseApp firebaseApp) {
        this.firebaseApp = firebaseApp;
    }

    public void send(final String fcmToken, final String title, final String body, final Map<String, String> data) {
        if (firebaseApp == null) {
            log.warn("FirebaseApp not initialized, skipping send");
            return;
        }
        if (!StringUtils.hasText(fcmToken)) {
            log.warn("FCM token is blank, skipping send");
            return;
        }
        try {
            final Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .build();
            final String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM message sent: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM message to token {}: {}", fcmToken, e.getMessage(), e);
        }
    }

    public void sendMulticast(final List<String> tokens, final String title, final String body, final Map<String, String> data) {
        if (firebaseApp == null) {
            log.warn("FirebaseApp not initialized, skipping multicast");
            return;
        }
        final List<String> validTokens = tokens.stream()
                .filter(StringUtils::hasText)
                .toList();
        if (validTokens.isEmpty()) {
            log.warn("No valid FCM tokens, skipping multicast");
            return;
        }
        try {
            final MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(validTokens)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .build();
            final BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("FCM multicast sent: {} success, {} failed",
                    response.getSuccessCount(), response.getFailureCount());
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM multicast: {}", e.getMessage(), e);
        }
    }
}
