package dev.batuhanyetgin.notificationservice.consumer;

import dev.batuhanyetgin.commonmodule.event.DebtAddedEvent;
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
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebtAddedConsumerTest {

    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private FcmService fcmService;
    @Mock private MailService mailService;
    @Mock private MessageSource messageSource;

    @InjectMocks
    private DebtAddedConsumer consumer;

    private static final String DEBT_ID   = "debt123";
    private static final String MEMBER_1  = "member1";
    private static final String MEMBER_2  = "member2";
    private static final String FCM_TOKEN_1 = "fcm-token-1";
    private static final String FCM_TOKEN_2 = "fcm-token-2";

    private DebtAddedEvent buildEvent(final List<String> memberIds) {
        return DebtAddedEvent.builder()
                .debtId(DEBT_ID)
                .debtName("Test Debt")
                .amount(BigDecimal.TEN)
                .memberUserIds(memberIds)
                .locale("en")
                .build();
    }

    private DeviceTokenDocument buildTokenDoc(final String userId, final String token) {
        return DeviceTokenDocument.builder().userId(userId).fcmToken(token).build();
    }

    private DeviceTokenDocument buildTokenDocWithEmail(final String userId, final String token,
                                                        final String email, final String locale) {
        return DeviceTokenDocument.builder()
                .userId(userId).fcmToken(token).email(email).locale(locale).build();
    }

    @Nested
    @DisplayName("consume")
    class Consume {

        @Test
        @DisplayName("members have tokens → sends FCM multicast")
        void membersHaveTokens_sendsFcmMulticast() {
            when(deviceTokenRepository.findByUserIdIn(List.of(MEMBER_1, MEMBER_2)))
                    .thenReturn(List.of(buildTokenDoc(MEMBER_1, FCM_TOKEN_1), buildTokenDoc(MEMBER_2, FCM_TOKEN_2)));
            when(messageSource.getMessage(eq("push.debt_added.title"), any(), any(Locale.class))).thenReturn("New Debt");
            when(messageSource.getMessage(eq("push.debt_added.body"), any(Object[].class), any(Locale.class))).thenReturn("Body");

            consumer.consume(buildEvent(List.of(MEMBER_1, MEMBER_2)));

            verify(fcmService).sendMulticast(
                    argThat(tokens -> tokens.containsAll(List.of(FCM_TOKEN_1, FCM_TOKEN_2))),
                    eq("New Debt"), eq("Body"), argThat(d -> d.containsKey("debtId")));
        }

        @Test
        @DisplayName("null memberUserIds → early return, no FCM call")
        void nullMemberIds_earlyReturn() {
            consumer.consume(buildEvent(null));

            verifyNoInteractions(fcmService);
            verifyNoInteractions(deviceTokenRepository);
        }

        @Test
        @DisplayName("empty memberUserIds → early return, no FCM call")
        void emptyMemberIds_earlyReturn() {
            consumer.consume(buildEvent(List.of()));

            verifyNoInteractions(fcmService);
            verifyNoInteractions(deviceTokenRepository);
        }

        @Test
        @DisplayName("members have no registered tokens → no FCM call")
        void noTokensRegistered_noFcmCall() {
            when(deviceTokenRepository.findByUserIdIn(List.of(MEMBER_1)))
                    .thenReturn(List.of());

            consumer.consume(buildEvent(List.of(MEMBER_1)));

            verifyNoInteractions(fcmService);
        }

        @Test
        @DisplayName("locale from event is used for FCM message resolution")
        void eventLocale_usedForMessages() {
            when(deviceTokenRepository.findByUserIdIn(any())).thenReturn(List.of(buildTokenDoc(MEMBER_1, FCM_TOKEN_1)));
            when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("msg");

            consumer.consume(buildEvent(List.of(MEMBER_1)));

            verify(messageSource, atLeastOnce())
                    .getMessage(anyString(), any(), eq(Locale.forLanguageTag("en")));
        }

        @Test
        @DisplayName("member with email → sends email using doc locale")
        void memberWithEmail_sendsEmail() {
            final String memberEmail = "member@example.com";
            when(deviceTokenRepository.findByUserIdIn(List.of(MEMBER_1)))
                    .thenReturn(List.of(buildTokenDocWithEmail(MEMBER_1, null, memberEmail, "tr")));
            when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("msg");

            consumer.consume(buildEvent(List.of(MEMBER_1)));

            verify(mailService).send(eq(memberEmail), anyString(), anyString());
        }

        @Test
        @DisplayName("member with FCM and email → sends both FCM multicast and email")
        void memberWithFcmAndEmail_sendsBoth() {
            final String memberEmail = "member@example.com";
            when(deviceTokenRepository.findByUserIdIn(List.of(MEMBER_1)))
                    .thenReturn(List.of(buildTokenDocWithEmail(MEMBER_1, FCM_TOKEN_1, memberEmail, "en")));
            when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("msg");

            consumer.consume(buildEvent(List.of(MEMBER_1)));

            verify(fcmService).sendMulticast(any(), any(), any(), any());
            verify(mailService).send(eq(memberEmail), anyString(), anyString());
        }

        @Test
        @DisplayName("mail send throws → exception caught silently")
        void mailSendThrows_caughtSilently() {
            final String memberEmail = "member@example.com";
            when(deviceTokenRepository.findByUserIdIn(List.of(MEMBER_1)))
                    .thenReturn(List.of(buildTokenDocWithEmail(MEMBER_1, null, memberEmail, "en")));
            when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("msg");
            doThrow(new RuntimeException("SMTP error")).when(mailService).send(any(), any(), any());

            org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                    () -> consumer.consume(buildEvent(List.of(MEMBER_1))));
        }

        @Test
        @DisplayName("member doc locale used for email, not event locale")
        void memberDocLocale_usedForEmail() {
            final String memberEmail = "member@example.com";
            when(deviceTokenRepository.findByUserIdIn(List.of(MEMBER_1)))
                    .thenReturn(List.of(buildTokenDocWithEmail(MEMBER_1, null, memberEmail, "tr")));
            when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("msg");

            consumer.consume(buildEvent(List.of(MEMBER_1))); // event locale is "en"

            verify(messageSource, atLeastOnce())
                    .getMessage(anyString(), any(Object[].class), eq(Locale.forLanguageTag("tr")));
        }
    }
}
