package dev.batuhanyetgin.notificationservice.controller;

import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import dev.batuhanyetgin.notificationservice.document.DeviceTokenDocument;
import dev.batuhanyetgin.notificationservice.dto.request.DeviceTokenRequest;
import dev.batuhanyetgin.notificationservice.dto.request.NotificationTestRequest;
import dev.batuhanyetgin.notificationservice.repository.DeviceTokenRepository;
import dev.batuhanyetgin.notificationservice.service.FcmService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceTokenControllerTest {

    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private FcmService fcmService;

    @InjectMocks
    private DeviceTokenController controller;

    private static final String USER_ID = "user123";
    private static final String FCM_TOKEN = "fcm-token-abc";
    private static final String LOCALE = "tr";

    @Nested
    class SendTestNotification {

        @Test
        void tokenExists_callsFcmAndReturns204() {
            final DeviceTokenDocument doc = DeviceTokenDocument.builder()
                    .userId(USER_ID).fcmToken(FCM_TOKEN).build();
            when(deviceTokenRepository.findByUserId(USER_ID)).thenReturn(Optional.of(doc));

            final NotificationTestRequest req = NotificationTestRequest.builder()
                    .userId(USER_ID).title("Hello").body("World").build();

            final ResponseEntity<Void> response = controller.sendTestNotification(req);

            assertThat(response.getStatusCode().value()).isEqualTo(204);
            verify(fcmService).send(eq(FCM_TOKEN), eq("Hello"), eq("World"), any(Map.class));
        }

        @Test
        void tokenNotFound_throwsRuntimeException() {
            when(deviceTokenRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            final NotificationTestRequest req = NotificationTestRequest.builder()
                    .userId(USER_ID).title("Hello").body("World").build();

            assertThatThrownBy(() -> controller.sendTestNotification(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(USER_ID);
        }
    }

    @Nested
    class RegisterDeviceToken {

        @Test
        void upsertsByUserIdAndReturns204() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(USER_ID);
                ctx.when(RequestContextUtil::getLocale).thenReturn(LOCALE);

                final DeviceTokenRequest req = new DeviceTokenRequest();
                req.setToken(FCM_TOKEN);
                req.setEmail("test@example.com");

                final ResponseEntity<Void> response = controller.registerDeviceToken(req);

                assertThat(response.getStatusCode().value()).isEqualTo(204);
                verify(deviceTokenRepository).upsertByUserId(USER_ID, FCM_TOKEN, LOCALE, "test@example.com");
            }
        }
    }

    @Nested
    class DeleteDeviceToken {

        @Test
        void deletesByUserIdAndReturns204() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(USER_ID);

                final ResponseEntity<Void> response = controller.deleteDeviceToken();

                assertThat(response.getStatusCode().value()).isEqualTo(204);
                verify(deviceTokenRepository).deleteByUserId(USER_ID);
            }
        }
    }
}
