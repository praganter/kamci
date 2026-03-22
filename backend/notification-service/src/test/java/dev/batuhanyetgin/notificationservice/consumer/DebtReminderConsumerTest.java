package dev.batuhanyetgin.notificationservice.consumer;

import dev.batuhanyetgin.commonmodule.event.DebtReminderEvent;
import dev.batuhanyetgin.commonmodule.event.ReminderChannelType;
import dev.batuhanyetgin.notificationservice.document.DeviceTokenDocument;
import dev.batuhanyetgin.notificationservice.repository.DeviceTokenRepository;
import dev.batuhanyetgin.notificationservice.service.FcmService;
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
    @Mock private MessageSource messageSource;

    @InjectMocks
    private DebtReminderConsumer consumer;

    private static final String DEBT_ID   = "debt123";
    private static final String OWNER_ID  = "owner123";
    private static final String FCM_TOKEN = "fcm-token-owner";

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

    private DeviceTokenDocument buildTokenDoc(final String locale) {
        return DeviceTokenDocument.builder()
                .userId(OWNER_ID).fcmToken(FCM_TOKEN).locale(locale).build();
    }

    @Nested
    @DisplayName("consume")
    class Consume {

        @Test
        @DisplayName("IN_APP channel + token registered → sends FCM push")
        void inAppChannel_tokenRegistered_sendsFcm() {
            when(deviceTokenRepository.findByUserId(OWNER_ID))
                    .thenReturn(Optional.of(buildTokenDoc("en")));
            when(messageSource.getMessage(eq("push.reminder.title"), any(Object[].class), any(Locale.class))).thenReturn("Reminder");
            when(messageSource.getMessage(eq("push.reminder.body"), any(Object[].class), any(Locale.class))).thenReturn("Body");

            consumer.consume(buildEvent(List.of(ReminderChannelType.IN_APP)));

            verify(fcmService).send(eq(FCM_TOKEN), eq("Reminder"), eq("Body"),
                    argThat(d -> d.containsKey("debtId")));
        }

        @Test
        @DisplayName("IN_APP channel + no token → no FCM call, no exception")
        void inAppChannel_noToken_noFcmCall() {
            when(deviceTokenRepository.findByUserId(OWNER_ID)).thenReturn(Optional.empty());

            consumer.consume(buildEvent(List.of(ReminderChannelType.IN_APP)));

            verifyNoInteractions(fcmService);
        }

        @Test
        @DisplayName("null channels → early return, no FCM call")
        void nullChannels_earlyReturn() {
            consumer.consume(buildEvent(null));

            verifyNoInteractions(fcmService);
            verifyNoInteractions(deviceTokenRepository);
        }

        @Test
        @DisplayName("empty channels → early return, no FCM call")
        void emptyChannels_earlyReturn() {
            consumer.consume(buildEvent(List.of()));

            verifyNoInteractions(fcmService);
            verifyNoInteractions(deviceTokenRepository);
        }

        @Test
        @DisplayName("token doc locale used for message resolution")
        void tokenDocLocale_usedForMessages() {
            when(deviceTokenRepository.findByUserId(OWNER_ID))
                    .thenReturn(Optional.of(buildTokenDoc("tr")));
            when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class))).thenReturn("msg");

            consumer.consume(buildEvent(List.of(ReminderChannelType.IN_APP)));

            verify(messageSource, atLeastOnce())
                    .getMessage(anyString(), any(Object[].class), eq(Locale.forLanguageTag("tr")));
        }

        @Test
        @DisplayName("blank token doc locale falls back to 'tr'")
        void blankTokenLocale_fallsBackToTr() {
            when(deviceTokenRepository.findByUserId(OWNER_ID))
                    .thenReturn(Optional.of(buildTokenDoc("")));
            when(messageSource.getMessage(anyString(), any(Object[].class), any(Locale.class))).thenReturn("msg");

            consumer.consume(buildEvent(List.of(ReminderChannelType.IN_APP)));

            verify(messageSource, atLeastOnce())
                    .getMessage(anyString(), any(Object[].class), eq(Locale.forLanguageTag("tr")));
        }
    }
}
