package dev.batuhanyetgin.notificationservice.consumer;

import dev.batuhanyetgin.commonmodule.event.HouseholdInviteEvent;
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

import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HouseholdInviteConsumerTest {

    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private FcmService fcmService;
    @Mock private MailService mailService;
    @Mock private MessageSource messageSource;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private HouseholdInviteConsumer consumer;

    private static final String HOUSEHOLD_ID = "household1";
    private static final String INVITER_EMAIL = "inviter@example.com";
    private static final String INVITEE_USER_ID = "invitee1";
    private static final String INVITEE_EMAIL = "invitee@example.com";
    private static final String FCM_TOKEN = "fcm-token-1";

    private HouseholdInviteEvent buildEvent() {
        return HouseholdInviteEvent.builder()
                .householdId(HOUSEHOLD_ID)
                .inviterUserId("inviter1")
                .inviterEmail(INVITER_EMAIL)
                .inviteeUserId(INVITEE_USER_ID)
                .inviteeEmail(INVITEE_EMAIL)
                .build();
    }

    private void stubMessages() {
        lenient().when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("msg");
    }

    @Nested
    @DisplayName("consume — invitee has a registered device token")
    class WithDeviceToken {

        @Test
        @DisplayName("token has FCM + email → sends FCM, email, and persists in-app notification")
        void tokenWithFcmAndEmail_sendsBothAndPersists() {
            stubMessages();
            when(deviceTokenRepository.findByUserId(INVITEE_USER_ID)).thenReturn(Optional.of(
                    DeviceTokenDocument.builder().userId(INVITEE_USER_ID).fcmToken(FCM_TOKEN)
                            .email(INVITEE_EMAIL).locale("en").build()));

            consumer.consume(buildEvent());

            verify(fcmService).send(eq(FCM_TOKEN), anyString(), anyString(),
                    argThat(m -> HOUSEHOLD_ID.equals(m.get("householdId"))));
            verify(mailService).send(eq(INVITEE_EMAIL), anyString(), anyString());
            verify(notificationService).persist(eq(INVITEE_USER_ID), eq(NotificationType.HOUSEHOLD_INVITE),
                    anyString(), anyString(), eq(HOUSEHOLD_ID));
        }

        @Test
        @DisplayName("token has no FCM token → no FCM call, email still sent via token doc email")
        void tokenWithoutFcm_noFcmCall() {
            stubMessages();
            when(deviceTokenRepository.findByUserId(INVITEE_USER_ID)).thenReturn(Optional.of(
                    DeviceTokenDocument.builder().userId(INVITEE_USER_ID).email(INVITEE_EMAIL).locale("en").build()));

            consumer.consume(buildEvent());

            verifyNoInteractions(fcmService);
            verify(mailService).send(eq(INVITEE_EMAIL), anyString(), anyString());
        }

        @Test
        @DisplayName("token doc has no email → falls back to event's inviteeEmail")
        void tokenWithoutEmail_fallsBackToEventEmail() {
            stubMessages();
            when(deviceTokenRepository.findByUserId(INVITEE_USER_ID)).thenReturn(Optional.of(
                    DeviceTokenDocument.builder().userId(INVITEE_USER_ID).fcmToken(FCM_TOKEN).locale("en").build()));

            consumer.consume(buildEvent());

            verify(mailService).send(eq(INVITEE_EMAIL), anyString(), anyString());
        }

        @Test
        @DisplayName("mail send throws → exception swallowed, in-app notification already persisted")
        void mailSendThrows_caughtSilently() {
            stubMessages();
            when(deviceTokenRepository.findByUserId(INVITEE_USER_ID)).thenReturn(Optional.of(
                    DeviceTokenDocument.builder().userId(INVITEE_USER_ID).email(INVITEE_EMAIL).locale("en").build()));
            doThrow(new RuntimeException("SMTP error")).when(mailService).send(any(), any(), any());

            assertDoesNotThrow(() -> consumer.consume(buildEvent()));

            verify(notificationService).persist(eq(INVITEE_USER_ID), eq(NotificationType.HOUSEHOLD_INVITE),
                    anyString(), anyString(), eq(HOUSEHOLD_ID));
        }

        @Test
        @DisplayName("no destination email at all → no mail call")
        void noDestinationEmail_noMailCall() {
            stubMessages();
            when(deviceTokenRepository.findByUserId(INVITEE_USER_ID)).thenReturn(Optional.of(
                    DeviceTokenDocument.builder().userId(INVITEE_USER_ID).fcmToken(FCM_TOKEN).locale("en").build()));

            final HouseholdInviteEvent event = HouseholdInviteEvent.builder()
                    .householdId(HOUSEHOLD_ID).inviterUserId("inviter1").inviterEmail(INVITER_EMAIL)
                    .inviteeUserId(INVITEE_USER_ID).inviteeEmail(null).build();

            consumer.consume(event);

            verifyNoInteractions(mailService);
        }
    }

    @Nested
    @DisplayName("consume — no device token record found")
    class WithoutDeviceToken {

        @Test
        @DisplayName("no token record → persists in-app notification and emails event's inviteeEmail")
        void noToken_persistsAndEmailsFallback() {
            stubMessages();
            when(deviceTokenRepository.findByUserId(INVITEE_USER_ID)).thenReturn(Optional.empty());

            consumer.consume(buildEvent());

            verifyNoInteractions(fcmService);
            verify(mailService).send(eq(INVITEE_EMAIL), anyString(), anyString());
            verify(notificationService).persist(eq(INVITEE_USER_ID), eq(NotificationType.HOUSEHOLD_INVITE),
                    anyString(), anyString(), eq(HOUSEHOLD_ID));
        }

        @Test
        @DisplayName("no token record and no inviteeEmail → no mail call, still persists")
        void noTokenNoEmail_noMailCall() {
            stubMessages();
            when(deviceTokenRepository.findByUserId(INVITEE_USER_ID)).thenReturn(Optional.empty());

            final HouseholdInviteEvent event = HouseholdInviteEvent.builder()
                    .householdId(HOUSEHOLD_ID).inviterUserId("inviter1").inviterEmail(INVITER_EMAIL)
                    .inviteeUserId(INVITEE_USER_ID).inviteeEmail(null).build();

            consumer.consume(event);

            verifyNoInteractions(mailService);
            verify(notificationService).persist(eq(INVITEE_USER_ID), eq(NotificationType.HOUSEHOLD_INVITE),
                    anyString(), anyString(), eq(HOUSEHOLD_ID));
        }

        @Test
        @DisplayName("no token record → mail send failure is swallowed")
        void noToken_mailSendThrows_caughtSilently() {
            stubMessages();
            when(deviceTokenRepository.findByUserId(INVITEE_USER_ID)).thenReturn(Optional.empty());
            doThrow(new RuntimeException("SMTP error")).when(mailService).send(any(), any(), any());

            assertDoesNotThrow(() -> consumer.consume(buildEvent()));
        }
    }
}
