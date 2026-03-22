package dev.batuhanyetgin.notificationservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock private JavaMailSender mailSender;

    private MailService mailService;

    @BeforeEach
    void setUp() {
        final SimpleMailMessage template = new SimpleMailMessage();
        template.setFrom("noreply@test.com");
        mailService = new MailService(mailSender, template);
    }

    @Test
    @DisplayName("send() sets to, subject, body then calls mailSender.send")
    void send_populatesMessageAndSends() {
        mailService.send("user@example.com", "Test Subject", "Test Body");

        final ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        final SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("user@example.com");
        assertThat(sent.getSubject()).isEqualTo("Test Subject");
        assertThat(sent.getText()).isEqualTo("Test Body");
    }

    @Test
    @DisplayName("send() calls mailSender exactly once")
    void send_callsMailSenderOnce() {
        mailService.send("a@b.com", "S", "B");
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
