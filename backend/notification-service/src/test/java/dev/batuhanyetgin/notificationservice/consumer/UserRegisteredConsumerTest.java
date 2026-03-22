package dev.batuhanyetgin.notificationservice.consumer;

import dev.batuhanyetgin.commonmodule.event.UserRegisteredEvent;
import dev.batuhanyetgin.notificationservice.document.DeviceTokenDocument;
import dev.batuhanyetgin.notificationservice.repository.DeviceTokenRepository;
import dev.batuhanyetgin.notificationservice.service.MailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegisteredConsumerTest {

    @Mock private MailService mailService;
    @Mock private MessageSource messageSource;
    @Mock private DeviceTokenRepository deviceTokenRepository;

    @InjectMocks
    private UserRegisteredConsumer consumer;

    private static final String USER_ID = "user123";
    private static final String EMAIL   = "user@example.com";
    private static final String LOCALE  = "en";

    private UserRegisteredEvent buildEvent() {
        return UserRegisteredEvent.builder()
                .userId(USER_ID)
                .email(EMAIL)
                .locale(LOCALE)
                .build();
    }

    @Nested
    @DisplayName("consume")
    class Consume {

        @Test
        @DisplayName("new user → creates DeviceTokenDocument and sends welcome mail")
        void newUser_createsTokenDocAndSendsMail() {
            when(deviceTokenRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(messageSource.getMessage(eq("mail.welcome.subject"), any(), any(Locale.class))).thenReturn("Welcome");
            when(messageSource.getMessage(eq("mail.welcome.body"), any(), any(Locale.class))).thenReturn("Hello!");

            consumer.consume(buildEvent());

            final ArgumentCaptor<DeviceTokenDocument> captor = ArgumentCaptor.forClass(DeviceTokenDocument.class);
            verify(deviceTokenRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
            assertThat(captor.getValue().getLocale()).isEqualTo(LOCALE);

            verify(mailService).send(EMAIL, "Welcome", "Hello!");
        }

        @Test
        @DisplayName("existing user → updates locale on existing document and sends welcome mail")
        void existingUser_updatesLocaleAndSendsMail() {
            final DeviceTokenDocument existing = DeviceTokenDocument.builder()
                    .userId(USER_ID).fcmToken("old-token").locale("tr").build();
            when(deviceTokenRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
            when(messageSource.getMessage(eq("mail.welcome.subject"), any(), any(Locale.class))).thenReturn("Welcome");
            when(messageSource.getMessage(eq("mail.welcome.body"), any(), any(Locale.class))).thenReturn("Hello!");

            consumer.consume(buildEvent());

            assertThat(existing.getLocale()).isEqualTo(LOCALE);
            verify(deviceTokenRepository).save(existing);
            verify(mailService).send(EMAIL, "Welcome", "Hello!");
        }

        @Test
        @DisplayName("mail is always sent regardless of token presence")
        void mailAlwaysSent() {
            when(deviceTokenRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("msg");

            consumer.consume(buildEvent());

            verify(mailService).send(eq(EMAIL), any(), any());
        }
    }
}
