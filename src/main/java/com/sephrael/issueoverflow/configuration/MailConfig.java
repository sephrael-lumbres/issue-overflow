package com.sephrael.issueoverflow.configuration;

import com.sephrael.issueoverflow.service.mail.EmailSender;
import com.sephrael.issueoverflow.service.mail.MailTransport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;

@Configuration
@EnableConfigurationProperties(MailSettings.class)
public class MailConfig {

    @Bean
    public EmailSender emailSender(MailTransport transport, MailSettings settings,
                                   @Qualifier(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME) Executor applicationTaskExecutor) {
        Executor executor = settings.async() ? applicationTaskExecutor : Runnable::run;
        return new EmailSender(transport, settings.enabled(), executor);
    }
}
