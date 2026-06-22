package dev.batuhanyetgin.notificationservice.consumer;

import dev.batuhanyetgin.commonmodule.event.CategoryMemberAddedEvent;
import dev.batuhanyetgin.notificationservice.document.DeviceTokenDocument;
import dev.batuhanyetgin.notificationservice.repository.DeviceTokenRepository;
import dev.batuhanyetgin.notificationservice.service.FcmService;
import dev.batuhanyetgin.notificationservice.service.MailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryMemberAddedConsumerTest {

    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private FcmService fcmService;
    @Mock private MailService mailService;
    @Mock private MessageSource messageSource;

    @InjectMocks
    private CategoryMemberAddedConsumer consumer;

    private static final String CATEGORY_ID   = "cat123";
    private static final String CATEGORY_NAME = "Test Category";
    private static final String OWNER_ID      = "owner123";
    private static final String MEMBER_ID     = "member456";
    private static final String FCM_TOKEN     = "fcm-token";
    private static final String EMAIL         = "member@example.com";

    private CategoryMemberAddedEvent buildEvent() {
        return CategoryMemberAddedEvent.builder()
                .categoryId(CATEGORY_ID)
                .categoryName(CATEGORY_NAME)
                .addedByUserId(OWNER_ID)
                .memberUserId(MEMBER_ID)
                .locale("en")
                .build();
    }

    private DeviceTokenDocument buildTokenDoc(final String fcmToken, final String email, final String locale) {
        return DeviceTokenDocument.builder()
                .userId(MEMBER_ID).fcmToken(fcmToken).email(email).locale(locale).build();
    }

    @Nested
    @DisplayName("consume")
    class Consume {

        @Test
        @DisplayName("FCM token registered → sends push notification")
        void fcmTokenRegistered_sendsPush() {
            when(deviceTokenRepository.findByUserId(MEMBER_ID))
                    .thenReturn(Optional.of(buildTokenDoc(FCM_TOKEN, null, "en")));
            when(messageSource.getMessage(eq("push.category_member_added.title"), any(), any(Locale.class))).thenReturn("Added");
            when(messageSource.getMessage(eq("push.category_member_added.body"), any(Object[].class), any(Locale.class))).thenReturn("Body");

            consumer.consume(buildEvent());

            verify(fcmService).send(eq(FCM_TOKEN), eq("Added"), eq("Body"),
                    argThat(d -> d.containsKey("categoryId")));
            verifyNoInteractions(mailService);
        }

        @Test
        @DisplayName("email registered → sends email notification")
        void emailRegistered_sendsEmail() {
            when(deviceTokenRepository.findByUserId(MEMBER_ID))
                    .thenReturn(Optional.of(buildTokenDoc(null, EMAIL, "en")));
            when(messageSource.getMessage(eq("mail.category_member_added.subject"), any(Object[].class), any(Locale.class))).thenReturn("Subject");
            when(messageSource.getMessage(eq("mail.category_member_added.body"), any(Object[].class), any(Locale.class))).thenReturn("Body");

            consumer.consume(buildEvent());

            verify(mailService).send(EMAIL, "Subject", "Body");
            verifyNoInteractions(fcmService);
        }

        @Test
        @DisplayName("both FCM and email registered → sends both")
        void bothRegistered_sendsBoth() {
            when(deviceTokenRepository.findByUserId(MEMBER_ID))
                    .thenReturn(Optional.of(buildTokenDoc(FCM_TOKEN, EMAIL, "en")));
            when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("msg");

            consumer.consume(buildEvent());

            verify(fcmService).send(eq(FCM_TOKEN), any(), any(), any());
            verify(mailService).send(eq(EMAIL), any(), any());
        }

        @Test
        @DisplayName("no device token record, no memberEmail → no notifications sent")
        void noTokenRecord_noNotifications() {
            when(deviceTokenRepository.findByUserId(MEMBER_ID)).thenReturn(Optional.empty());

            consumer.consume(buildEvent());

            verifyNoInteractions(fcmService);
            verifyNoInteractions(mailService);
        }

        @Test
        @DisplayName("no device token record but memberEmail set → sends fallback email with default locale")
        void noTokenRecord_memberEmailSet_sendsFallbackEmail() {
            when(deviceTokenRepository.findByUserId(MEMBER_ID)).thenReturn(Optional.empty());
            when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class))).thenReturn("msg");

            final CategoryMemberAddedEvent eventWithEmail = CategoryMemberAddedEvent.builder()
                    .categoryId(CATEGORY_ID)
                    .categoryName(CATEGORY_NAME)
                    .addedByUserId(OWNER_ID)
                    .memberUserId(MEMBER_ID)
                    .memberEmail(EMAIL)
                    .locale("en")
                    .build();

            consumer.consume(eventWithEmail);

            verify(mailService).send(eq(EMAIL), anyString(), anyString());
            verifyNoInteractions(fcmService);
        }

        @Test
        @DisplayName("no token record, memberEmail set, mail throws → caught silently")
        void noTokenRecord_mailThrows_caughtSilently() {
            when(deviceTokenRepository.findByUserId(MEMBER_ID)).thenReturn(Optional.empty());
            when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class))).thenReturn("msg");
            doThrow(new RuntimeException("SMTP error")).when(mailService).send(any(), any(), any());

            final CategoryMemberAddedEvent eventWithEmail = CategoryMemberAddedEvent.builder()
                    .categoryId(CATEGORY_ID)
                    .categoryName(CATEGORY_NAME)
                    .addedByUserId(OWNER_ID)
                    .memberUserId(MEMBER_ID)
                    .memberEmail(EMAIL)
                    .locale("en")
                    .build();

            assertDoesNotThrow(() -> consumer.consume(eventWithEmail));
        }

        @Test
        @DisplayName("mail send throws → exception caught, no propagation")
        void mailSendThrows_caughtSilently() {
            when(deviceTokenRepository.findByUserId(MEMBER_ID))
                    .thenReturn(Optional.of(buildTokenDoc(null, EMAIL, "en")));
            when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("msg");
            doThrow(new RuntimeException("SMTP error")).when(mailService).send(any(), any(), any());

            assertDoesNotThrow(() -> consumer.consume(buildEvent()));
        }
    }
}
