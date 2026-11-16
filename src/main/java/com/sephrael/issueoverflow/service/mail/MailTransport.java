package com.sephrael.issueoverflow.service.mail;

/**
 * Delivers an email. {@link SmtpMailTransport} uses SMTP (Mailpit locally); {@link ResendMailTransport} uses Resend's
 * HTTPS API, for a host that blocks SMTP. app.mail.transport picks one.
 */
public interface MailTransport {

    void send(Email email);
}
