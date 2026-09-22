package dev.batuhanyetgin.notificationservice.controller;

import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import dev.batuhanyetgin.notificationservice.document.NotificationDocument;
import dev.batuhanyetgin.notificationservice.document.NotificationType;
import dev.batuhanyetgin.notificationservice.dto.response.NotificationPageResponse;
import dev.batuhanyetgin.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock private NotificationService notificationService;

    @InjectMocks
    private NotificationController controller;

    private static final String USER_ID = "user123";
    private static final String NOTIFICATION_ID = "notif123";

    @Nested
    class ListNotifications {

        @Test
        void returnsPagedNotifications() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(USER_ID);

                final NotificationDocument doc = NotificationDocument.builder()
                        .userId(USER_ID).type(NotificationType.DEBT_REMINDER).title("t").body("b").read(false).build();
                when(notificationService.list(eq(USER_ID), eq(PageRequest.of(0, 20))))
                        .thenReturn(new PageImpl<>(List.of(doc)));

                final ResponseEntity<NotificationPageResponse> response = controller.list(0, 20);

                assertThat(response.getStatusCode().value()).isEqualTo(200);
                assertThat(response.getBody().getContent()).hasSize(1);
                assertThat(response.getBody().getContent().get(0).getTitle()).isEqualTo("t");
            }
        }
    }

    @Nested
    class MarkAsRead {

        @Test
        void delegatesToServiceAndReturns204() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(USER_ID);

                final ResponseEntity<Void> response = controller.markAsRead(NOTIFICATION_ID);

                assertThat(response.getStatusCode().value()).isEqualTo(204);
                verify(notificationService).markAsRead(NOTIFICATION_ID, USER_ID);
            }
        }
    }
}
