package com.sephrael.issueoverflow.configuration;

import com.sephrael.issueoverflow.service.mail.EmailSender;
import com.sephrael.issueoverflow.service.mail.MailTransport;
import com.sephrael.issueoverflow.service.mail.ResendMailTransport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.concurrent.Executor;

@Configuration
@EnableConfigurationProperties({MailSettings.class, ResendSettings.class})
public class MailConfig {

    @Bean
    public EmailSender emailSender(MailTransport transport, MailSettings settings,
                                   @Qualifier(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME) Executor applicationTaskExecutor) {
        Executor executor = settings.async() ? applicationTaskExecutor : Runnable::run;
        return new EmailSender(transport, settings.enabled(), executor);
    }

    // app.mail.transport=resend (the prod profile) sends through Resend's HTTPS API; otherwise SmtpMailTransport sends
    @Bean
    @ConditionalOnProperty(name = "app.mail.transport", havingValue = "resend")
    public MailTransport resendMailTransport(ResendSettings resend, MailSettings settings) {
        if(settings.enabled() && !StringUtils.hasText(resend.apiKey()))
            throw new IllegalStateException("app.mail.transport=resend needs an API key: set RESEND_API_KEY, "
                    + "or set APP_MAIL_ENABLED=false to log emails instead of sending them");

        return new ResendMailTransport(ResendMailTransport.restClientBuilder(), resend.apiKey(), resend.baseUrl());
    }
}
