package com.sephrael.issueoverflow.service;

import com.sephrael.issueoverflow.configuration.AppProperties;
import com.sephrael.issueoverflow.entity.Issue;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Absolute links for emails, built from {@code app.base-url}.
 */
@Component
public class EmailLinks {

    private final String baseUrl;

    public EmailLinks(AppProperties properties) {
        String url = properties.baseUrl().toString();
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public String passwordReset(String token) {
        return UriComponentsBuilder.fromUriString(baseUrl).path("/change-password")
                .queryParam("token", token).build().encode().toUriString();
    }

    public String emailVerification(String token) {
        return UriComponentsBuilder.fromUriString(baseUrl).path("/verify-email")
                .queryParam("emailVerificationToken", token).build().encode().toUriString();
    }

    public String issue(Issue issue) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .pathSegment("issues", issue.getProject().getIdentifier(), "view", issue.getIssueKey())
                .build().encode().toUriString();
    }
}
