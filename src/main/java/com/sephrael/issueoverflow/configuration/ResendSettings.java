package com.sephrael.issueoverflow.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

/**
 * Resend settings, bound from {@code app.mail.resend.*}; used only when {@code app.mail.transport=resend}.
 * <p>
 * {@code apiKey}: a sending-only API key, from RESEND_API_KEY. Startup fails without one while email is on.
 * {@code baseUrl}: Resend's API address; tests point it at a mock server.
 */
@ConfigurationProperties("app.mail.resend")
public record ResendSettings(String apiKey, URI baseUrl) {

    // Leaves out the API key, so logging this record can't leak it.
    @Override
    public String toString() {
        return "ResendSettings[baseUrl=" + baseUrl + "]";
    }
}
