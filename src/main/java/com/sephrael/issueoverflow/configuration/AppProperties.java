package com.sephrael.issueoverflow.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.net.URI;
import java.time.Duration;

/**
 * Application settings, bound from {@code app.*} (the S3 settings are in {@link S3Properties}).
 * <p>
 * {@code baseUrl} is the address users reach the app at, for example {@code https://issues.example.com}.
 * Links in emails are built from it rather than from the request, whose Host header the client chooses.
 * <p>
 * {@code passwordResetTtl} and {@code emailVerificationTtl} are how long the links in those emails work.
 */
@ConfigurationProperties("app")
public record AppProperties(URI baseUrl, Duration passwordResetTtl, Duration emailVerificationTtl) {

    public AppProperties {
        Assert.notNull(baseUrl, "app.base-url must be set");
        Assert.isTrue("http".equals(baseUrl.getScheme()) || "https".equals(baseUrl.getScheme()),
                "app.base-url must be an http or https URL");
        Assert.hasText(baseUrl.getHost(), "app.base-url must include a host");
        Assert.notNull(passwordResetTtl, "app.password-reset-ttl must be set");
        Assert.notNull(emailVerificationTtl, "app.email-verification-ttl must be set");
    }
}
