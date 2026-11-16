package com.sephrael.issueoverflow.service.mail;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * Sends email through Resend's HTTPS API (POST /emails), for a host that blocks outbound SMTP.
 * Resend takes at most 50 addresses in {@code to}, so a longer list goes out as several emails.
 * A refused or failed call throws a RestClientException, which EmailSender logs.
 */
public class ResendMailTransport implements MailTransport {

    static final int MAX_RECIPIENTS = 50;

    private final RestClient restClient;

    public ResendMailTransport(RestClient.Builder restClientBuilder, String apiKey, URI baseUrl) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl.toString())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                // Resend refuses requests without a User-Agent (403)
                .defaultHeader(HttpHeaders.USER_AGENT, "issue-overflow")
                .build();
    }

    // Gives up on a call that doesn't connect in 5 seconds or answer in 10, like the SMTP timeouts
    public static RestClient.Builder restClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().requestFactory(requestFactory);
    }

    @Override
    public void send(Email email) {
        String from = email.fromName() + " <" + email.from() + ">";
        List<String> to = email.to();

        for(int start = 0; start < to.size(); start += MAX_RECIPIENTS) {
            List<String> recipients = to.subList(start, Math.min(start + MAX_RECIPIENTS, to.size()));
            restClient.post()
                    .uri("/emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SendEmailRequest(from, recipients, email.subject(), email.html()))
                    .retrieve()
                    .toBodilessEntity();
        }
    }

    // The JSON body Resend expects; field names are Resend's
    record SendEmailRequest(String from, List<String> to, String subject, String html) {
    }
}
