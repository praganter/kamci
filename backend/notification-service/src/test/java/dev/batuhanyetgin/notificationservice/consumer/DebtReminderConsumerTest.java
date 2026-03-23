package dev.batuhanyetgin.notificationservice.consumer;

import dev.batuhanyetgin.commonmodule.event.DebtReminderEvent;
import dev.batuhanyetgin.commonmodule.event.ReminderChannelType;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebtReminderConsumerTest {

    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private FcmService fcmService;
    @Mock private MailService mailService;
    @Mock private MessageSource messageSource;

    @InjectMocks
    private DebtReminderConsumer consumer;

    private static final String DEBT_ID   = "debt123";
    private static final String OWNER_ID  = "owner123";
    private static final String FCM_TOKEN = "fcm-token-owner";
    private static final String EMAIL     = "owner@example.com";

    private DebtReminderEvent buildEvent(final List<ReminderChannelType> channels) {
        return DebtReminderEvent.builder()
                .debtId(DEBT_ID)
                .ownerId(OWNER_ID)
                .debtName("Test Debt")
                .amount(BigDecimal.TEN)
                .nextReminderDate(LocalDateTime.of(2026, 4, 1, 10, 0))
                .channels(channels)
                .locale("en")
                .build();
    }

    private DeviceTokenDocument buildTokenDoc(final String locale, final String email) {
        return DeviceTokenDocument.builder()
                .userId(OWNER_ID).fcmToken(FCM_TOKEN).locale(locale).email(email).build();
    }

    @Nested
    @DisplayName("IN_APP channel")
    class InApp {

        @Test
        @DisplayName("token registered → sends FCM push")
        void tokenRegistered_sendsFcm() {
            when(deviceTokenRepository.findByUserId(OWNER_ID))
                    .thenReturn(Optional.of(buildTokenDoc("en", null)));
            when(messageSource.getMessage(eq("push.reminder.title"), any(Object[].class), any(Locale.class))).thenReturn("Reminder");
            when(messageSource.getMessage(eq("push.reminder.body"), any(Object[].class), any(Locale.class))).thenReturn("Body");

            consumer.consume(buildEvent(List.of(ReminderChannelType.IN_APP)));

            verify(fcmService).send(eq(FCM_TOKEN), eq("Reminder"), eq("Body"),
                    argThat(d -> d.containsKey("debtId")));
            verifyNoInteractions(mailService);
        }

        @Test
        @DisplayName("no token record → no FCM call, no exception")
        void noTokenRecord_noFcmCall() {
            when(deviceTokenRepository.findByUserId(OWNER_ID)).thenReturn(Optional.empty());

            consumer.consume(buildEvent(List.of(ReminderChannelType.IN_APP)));

            verifyNoInteractions(fcmService);
            verifyNoInteractions(mailService);
        }

        @Test
        @DisplayName("token doc locale used for message resolution")
        void tokenDocLocale_usedForMessages() {
            when(deviceTokenRepository.findByUserId(OWNER_ID))
                    .thenReturn(Optional.of(buildTokenDoc("tr", null)));
            when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class))).thenReturn("msg");

            consumer.consume(buildEvent(List.of(ReminderChannelType.IN_APP)));

            verify(messageSource, atLeastOnce())
                    .getMessage(anyString(), any(Object[].class), eq(Locale.forLanguageTag("tr")));
        }

        @Test
        @DisplayName("blank token doc locale falls back to 'tr'")
        void blankTokenLocale_fallsBackToTr() {
            when(deviceTokenRepository.findByUserId(OWNER_ID))
                    .thenReturn(Optional.of(buildTokenDoc("", null)));
            when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class))).thenReturn("msg");

            consumer.consume(buildEvent(List.of(ReminderChannelType.IN_APP)));

            verify(messageSource, atLeastOnce())
                    .getMessage(anyString(), any(Object[].class), eq(Locale.forLanguageTag("tr")));
        }
    }

    @Nested
    @DisplayName("EMAIL channel")
    class Email {

        @Test
        @DisplayName("email registered → sends reminder email")
        void emailRegistered_sendsEmail() {
            when(deviceTokenRepository.findByUserId(OWNER_ID))
                    .thenReturn(Optional.of(buildTokenDoc("en", EMAIL)));
            when(messageSource.getMessage(eq("mail.reminder.subject"), any(Object[].class), any(Locale.class))).thenReturn("Reminder Subject");
            when(messageSource.getMessage(eq("mail.reminder.body"), any(Object[].class), any(Locale.class))).thenReturn("Reminder Body");

            consumer.consume(buildEvent(List.of(ReminderChannelType.EMAIL)));

            verify(mailService).send(EMAIL, "Reminder Subject", "Reminder Body");
            verifyNoInteractions(fcmService);
        }

        @Test
        @DisplayName("no email in token doc → no email sent")
        void noEmail_noMailCall() {
            when(deviceTokenRepository.findByUserId(OWNER_ID))
                    .thenReturn(Optional.of(buildTokenDoc("en", null)));

            consumer.consume(buildEvent(List.of(ReminderChannelType.EMAIL)));

            verifyNoInteractions(mailService);
            verifyNoInteractions(fcmService);
        }

        @Test
        @DisplayName("mail send throws → exception is caught, no propagation")
        void mailSendThrows_caughtSilently() throws Exception {
            when(deviceTokenRepository.findByUserId(OWNER_ID))
                    .thenReturn(Optional.of(buildTokenDoc("en", EMAIL)));
            when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class))).thenReturn("msg");
            doThrow(new RuntimeException("SMTP error")).when(mailService).send(any(), any(), any());

            org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                    () -> consumer.consume(buildEvent(List.of(ReminderChannelType.EMAIL))));
        }
    }

    @Nested
    @DisplayName("both channels")
    class BothChannels {

        @Test
        @DisplayName("IN_APP + EMAIL → sends both FCM and email")
        void bothChannels_sendsBoth() {
            when(deviceTokenRepository.findByUserId(OWNER_ID))
                    .thenReturn(Optional.of(buildTokenDoc("en", EMAIL)));
            when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class))).thenReturn("msg");

            consumer.consume(buildEvent(List.of(ReminderChannelType.IN_APP, ReminderChannelType.EMAIL)));

            verify(fcmService).send(eq(FCM_TOKEN), any(), any(), any());
            verify(mailService).send(eq(EMAIL), any(), any());
        }
    }

    @Nested
    @DisplayName("empty / null channels")
    class NoChannels {

        @Test
        @DisplayName("null channels → early return, no interactions")
        void nullChannels_earlyReturn() {
            consumer.consume(buildEvent(null));

            verifyNoInteractions(fcmService);
            verifyNoInteractions(mailService);
            verifyNoInteractions(deviceTokenRepository);
        }

        @Test
        @DisplayName("empty channels → early return, no interactions")
        void emptyChannels_earlyReturn() {
            consumer.consume(buildEvent(List.of()));

            verifyNoInteractions(fcmService);
            verifyNoInteractions(mailService);
            verifyNoInteractions(deviceTokenRepository);
        }
    }
}
