package com.sephrael.issueoverflow.service.mail;

/**
 * Delivers an email. {@link SmtpMailTransport} uses SMTP (the configured mail server, or Mailpit locally);
 * a provider's HTTPS API can implement this too, for a host that blocks SMTP.
 */
public interface MailTransport {

    void send(Email email);
}
