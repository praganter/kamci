package dev.batuhanyetgin.notificationservice.consumer;

import dev.batuhanyetgin.commonmodule.event.PasswordResetEvent;
import dev.batuhanyetgin.notificationservice.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Locale;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetConsumerTest {

    @Mock private MailService mailService;
    @Mock private MessageSource messageSource;

    private PasswordResetConsumer consumer;

    private static final String EMAIL       = "user@example.com";
    private static final String RESET_TOKEN = "reset.jwt.token";
    private static final String DEEP_LINK   = "myapp://reset-password";

    @BeforeEach
    void setUp() {
        consumer = new PasswordResetConsumer(mailService, messageSource);
        ReflectionTestUtils.setField(consumer, "passwordResetDeepLink", DEEP_LINK);
    }

    @Test
    @DisplayName("consume → resolves messages with locale and sends reset mail")
    void consume_sendsPasswordResetMail() {
        when(messageSource.getMessage(eq("mail.password_reset.subject"), any(), any(Locale.class)))
                .thenReturn("Reset your password");
        when(messageSource.getMessage(eq("mail.password_reset.body"), any(Object[].class), any(Locale.class)))
                .thenReturn("Click here: " + DEEP_LINK + "?token=" + RESET_TOKEN);

        final PasswordResetEvent event = PasswordResetEvent.builder()
                .userId("user123")
                .email(EMAIL)
                .resetToken(RESET_TOKEN)
                .locale("en")
                .build();

        consumer.consume(event);

        verify(mailService).send(eq(EMAIL), eq("Reset your password"), contains(RESET_TOKEN));
    }

    @Test
    @DisplayName("consume with TR locale → uses Turkish locale for message resolution")
    void consume_trLocale_usesTurkish() {
        when(messageSource.getMessage(anyString(), any(), eq(Locale.forLanguageTag("tr"))))
                .thenReturn("msg");

        final PasswordResetEvent event = PasswordResetEvent.builder()
                .userId("user123").email(EMAIL).resetToken(RESET_TOKEN).locale("tr").build();

        consumer.consume(event);

        verify(messageSource, atLeastOnce())
                .getMessage(anyString(), any(), eq(Locale.forLanguageTag("tr")));
    }

    @Test
    @DisplayName("consume → deep link and reset token are passed to message body")
    void consume_passesDeepLinkAndTokenToBody() {
        consumer.consume(PasswordResetEvent.builder()
                .userId("u1").email(EMAIL).resetToken(RESET_TOKEN).locale("en").build());

        verify(messageSource).getMessage(
                eq("mail.password_reset.body"),
                argThat(args -> args != null
                        && ((Object[]) args)[0].equals(DEEP_LINK)
                        && ((Object[]) args)[1].equals(RESET_TOKEN)),
                any(Locale.class));
    }
}
