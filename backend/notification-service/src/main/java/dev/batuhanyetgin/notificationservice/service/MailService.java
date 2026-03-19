package dev.batuhanyetgin.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final SimpleMailMessage templateMessage;

    public void send(final String to, final String subject, final String body) {
        final SimpleMailMessage message = new SimpleMailMessage(templateMessage);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
        log.info("Mail sent to: {}", to);
    }
}
