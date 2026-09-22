package dev.batuhanyetgin.notificationservice.consumer;

import dev.batuhanyetgin.commonmodule.event.WeeklyDigestEvent;
import dev.batuhanyetgin.commonmodule.event.WeeklyDigestItem;
import dev.batuhanyetgin.notificationservice.document.DeviceTokenDocument;
import dev.batuhanyetgin.notificationservice.document.NotificationType;
import dev.batuhanyetgin.notificationservice.repository.DeviceTokenRepository;
import dev.batuhanyetgin.notificationservice.service.FcmService;
import dev.batuhanyetgin.notificationservice.service.MailService;
import dev.batuhanyetgin.notificationservice.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeeklyDigestConsumerTest {

    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private FcmService fcmService;
    @Mock private MailService mailService;
    @Mock private MessageSource messageSource;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private WeeklyDigestConsumer consumer;

    private static final String OWNER_ID  = "owner123";
    private static final String FCM_TOKEN = "fcm-token-owner";
    private static final String EMAIL     = "owner@example.com";

    private WeeklyDigestEvent buildEvent(final List<WeeklyDigestItem> items) {
        return WeeklyDigestEvent.builder()
                .ownerId(OWNER_ID)
                .weekStart(LocalDateTime.of(2026, 4, 6, 9, 0))
                .weekEnd(LocalDateTime.of(2026, 4, 13, 9, 0))
                .totalAmount(BigDecimal.valueOf(250))
                .items(items)
                .build();
    }

    private WeeklyDigestItem buildItem() {
        return WeeklyDigestItem.builder()
                .debtId("debt1")
                .debtName("Rent")
                .amount(BigDecimal.valueOf(250))
                .dueDate(LocalDateTime.of(2026, 4, 10, 9, 0))
                .build();
    }

    private DeviceTokenDocument buildTokenDoc(final String fcmToken, final String email, final String locale) {
        return DeviceTokenDocument.builder()
                .userId(OWNER_ID).fcmToken(fcmToken).email(email).locale(locale).build();
    }

    @Nested
    @DisplayName("empty / null items")
    class NoItems {

        @Test
        @DisplayName("null items → early return, no interactions")
        void nullItems_earlyReturn() {
            consumer.consume(buildEvent(null));

            verifyNoInteractions(fcmService, mailService, deviceTokenRepository, notificationService);
        }

        @Test
        @DisplayName("empty items → early return, no interactions")
        void emptyItems_earlyReturn() {
            consumer.consume(buildEvent(List.of()));

            verifyNoInteractions(fcmService, mailService, deviceTokenRepository, notificationService);
        }
    }

    @Nested
    @DisplayName("consume")
    class Consume {

        @Test
        @DisplayName("token registered → persists notification and sends FCM push")
        void tokenRegistered_persistsAndSendsFcm() {
            when(deviceTokenRepository.findByUserId(OWNER_ID))
                    .thenReturn(Optional.of(buildTokenDoc(FCM_TOKEN, null, "en")));
            when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("msg");

            consumer.consume(buildEvent(List.of(buildItem())));

            verify(notificationService).persist(eq(OWNER_ID), eq(NotificationType.WEEKLY_DIGEST), anyString(), anyString(), isNull());
            verify(fcmService).send(eq(FCM_TOKEN), anyString(), anyString(), anyMap());
            verifyNoInteractions(mailService);
        }

        @Test
        @DisplayName("no device token → notification still persisted, no push/email")
        void noTokenRecord_persistsOnly() {
            when(deviceTokenRepository.findByUserId(OWNER_ID)).thenReturn(Optional.empty());
            when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("msg");

            consumer.consume(buildEvent(List.of(buildItem())));

            verify(notificationService).persist(eq(OWNER_ID), eq(NotificationType.WEEKLY_DIGEST), anyString(), anyString(), isNull());
            verifyNoInteractions(fcmService);
            verifyNoInteractions(mailService);
        }

        @Test
        @DisplayName("email registered → sends digest email")
        void emailRegistered_sendsEmail() {
            when(deviceTokenRepository.findByUserId(OWNER_ID))
                    .thenReturn(Optional.of(buildTokenDoc(null, EMAIL, "en")));
            when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("msg");

            consumer.consume(buildEvent(List.of(buildItem())));

            verify(mailService).send(eq(EMAIL), anyString(), anyString());
            verifyNoInteractions(fcmService);
        }

        @Test
        @DisplayName("mail send throws → exception caught, no propagation")
        void mailSendThrows_caughtSilently() {
            when(deviceTokenRepository.findByUserId(OWNER_ID))
                    .thenReturn(Optional.of(buildTokenDoc(null, EMAIL, "en")));
            when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("msg");
            doThrow(new RuntimeException("SMTP error")).when(mailService).send(any(), any(), any());

            assertDoesNotThrow(() -> consumer.consume(buildEvent(List.of(buildItem()))));
        }
    }
}
