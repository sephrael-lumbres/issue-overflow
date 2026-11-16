package com.sephrael.issueoverflow.service.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;

/**
 * Sends email through Spring's {@link JavaMailSender} (spring.mail.*), unless app.mail.transport names another transport.
 */
@Component
@ConditionalOnProperty(name = "app.mail.transport", havingValue = "smtp", matchIfMissing = true)
public class SmtpMailTransport implements MailTransport {

    private final JavaMailSender mailSender;

    public SmtpMailTransport(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(Email email) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message);
            helper.setFrom(email.from(), email.fromName());
            helper.setTo(email.to().toArray(String[]::new));
            helper.setSubject(email.subject());
            helper.setText(email.html(), true);
        } catch (MessagingException | UnsupportedEncodingException exception) {
            throw new MailPreparationException("Could not build the email \"" + email.subject() + "\"", exception);
        }

        mailSender.send(message);
    }
}
