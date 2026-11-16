package com.sephrael.issueoverflow.configuration;

import com.sephrael.issueoverflow.service.mail.MailTransport;
import com.sephrael.issueoverflow.service.mail.ResendMailTransport;
import com.sephrael.issueoverflow.service.mail.SmtpMailTransport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * app.mail.transport picks the transport; MailConfig and SmtpMailTransport alone, with no web server or database.
 */
class MailTransportSelectionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(MailConfig.class, SmtpMailTransport.class)
            .withBean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME, Executor.class,
                    () -> (Executor) Runnable::run)
            .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class));

    @Test
    void smtpSendsWhenNoTransportIsNamed() {
        contextRunner.run(context ->
                assertThat(context.getBean(MailTransport.class)).isInstanceOf(SmtpMailTransport.class));
    }

    @Test
    void resendSendsWhenNamed() {
        contextRunner
                .withPropertyValues("app.mail.enabled=true", "app.mail.transport=resend",
                        "app.mail.resend.api-key=re_test_key", "app.mail.resend.base-url=https://api.resend.test")
                .run(context -> {
                    assertThat(context).hasSingleBean(MailTransport.class);
                    assertThat(context.getBean(MailTransport.class)).isInstanceOf(ResendMailTransport.class);
                });
    }

    @Test
    void resendWithoutAKeyFailsAtStartupWhileEmailIsOn() {
        contextRunner
                .withPropertyValues("app.mail.enabled=true", "app.mail.transport=resend",
                        "app.mail.resend.base-url=https://api.resend.test")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("RESEND_API_KEY"));
    }

    // the first deployment runs with email off and no key yet
    @Test
    void resendWithoutAKeyStartsWhileEmailIsOff() {
        contextRunner
                .withPropertyValues("app.mail.enabled=false", "app.mail.transport=resend",
                        "app.mail.resend.base-url=https://api.resend.test")
                .run(context -> assertThat(context).hasSingleBean(ResendMailTransport.class));
    }
}
