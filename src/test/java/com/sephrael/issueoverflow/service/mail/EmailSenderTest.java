package com.sephrael.issueoverflow.service.mail;

import com.sephrael.issueoverflow.configuration.MailConfig;
import com.sephrael.issueoverflow.configuration.MailSettings;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class EmailSenderTest {

    private static final Email EMAIL = new Email("support@issueoverflow.app", "Issue Overflow",
            List.of("user@example.test"), "Subject", "<p>Body</p>");

    private final List<Email> sent = new ArrayList<>();
    private final MailTransport transport = sent::add;
    // runs nothing until the test says so, like a background thread that hasn't started yet
    private final List<Runnable> queued = new ArrayList<>();
    private final Executor laterExecutor = queued::add;

    @Test
    void anEmailIsSentAfterTheCallReturnsWhenAsync() {
        EmailSender sender = new MailConfig().emailSender(transport, new MailSettings(true, true), laterExecutor);

        sender.send(EMAIL);
        assertThat(sent).isEmpty();

        queued.forEach(Runnable::run);
        assertThat(sent).containsExactly(EMAIL);
    }

    @Test
    void anEmailIsSentDuringTheCallWhenNotAsync() {
        EmailSender sender = new MailConfig().emailSender(transport, new MailSettings(true, false), laterExecutor);

        sender.send(EMAIL);

        assertThat(sent).containsExactly(EMAIL);
        assertThat(queued).isEmpty();
    }

    @Test
    void aFailedSendIsLoggedNotThrown() {
        MailTransport failing = email -> {
            throw new MailSendException("SMTP server unavailable");
        };
        EmailSender sender = new EmailSender(failing, true, Runnable::run);

        assertThatCode(() -> sender.send(EMAIL)).doesNotThrowAnyException();
    }

    @Test
    void aFullQueueIsLoggedNotThrown() {
        Executor rejecting = task -> {
            throw new RejectedExecutionException("queue full");
        };
        EmailSender sender = new EmailSender(transport, true, rejecting);

        assertThatCode(() -> sender.send(EMAIL)).doesNotThrowAnyException();
        assertThat(sent).isEmpty();
    }

    @Test
    void nothingIsSentWhenEmailIsTurnedOff() {
        EmailSender sender = new EmailSender(transport, false, laterExecutor);

        sender.send(EMAIL);

        assertThat(sent).isEmpty();
        assertThat(queued).isEmpty();
    }
}
