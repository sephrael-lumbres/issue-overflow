package com.sephrael.issueoverflow.service.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Sends email outside the request: {@link #send} hands the email to an executor and returns, so a page doesn't wait
 * for the mail server or fail when it is down. A failed send, or an email the executor's full queue refuses, is
 * logged, never thrown. Built by MailConfig.
 */
public class EmailSender {

    private static final Logger logger = LoggerFactory.getLogger(EmailSender.class);

    private final MailTransport transport;
    private final boolean enabled;
    private final Executor executor;

    public EmailSender(MailTransport transport, boolean enabled, Executor executor) {
        this.transport = transport;
        this.enabled = enabled;
        this.executor = executor;
    }

    public void send(Email email) {
        if(!enabled) {
            logger.info("Email is turned off (app.mail.enabled=false); not sending \"{}\" to {} recipient(s)",
                    email.subject(), email.to().size());
            return;
        }

        try {
            executor.execute(() -> {
                try {
                    transport.send(email);
                } catch (RuntimeException exception) {
                    logger.warn("Could not send the email \"{}\"", email.subject(), exception);
                }
            });
        } catch (RejectedExecutionException exception) {
            logger.warn("The email queue is full; not sending \"{}\"", email.subject(), exception);
        }
    }
}
