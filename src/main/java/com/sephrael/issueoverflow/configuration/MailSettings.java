package com.sephrael.issueoverflow.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Email settings, bound from {@code app.mail.*}.
 * <p>
 * {@code enabled}: when false, emails are logged instead of sent (for a deployment without a mail provider).
 * {@code async}: when true, emails are sent on Spring Boot's application task executor after the request;
 * the test profile sets it to false, so tests see each email before the request returns.
 */
@ConfigurationProperties("app.mail")
public record MailSettings(boolean enabled, boolean async) {
}
