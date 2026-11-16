package com.sephrael.issueoverflow.service.mail;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResendMailTransportTest {

    private static final URI BASE_URL = URI.create("https://api.resend.test");
    private static final String SENT = "{\"id\":\"49a3999c-0ce1-4ea6-ab68-afcd6dc2e794\"}";

    private final RestClient.Builder restClientBuilder = RestClient.builder();
    private final MockRestServiceServer resend = MockRestServiceServer.bindTo(restClientBuilder).build();
    private final ResendMailTransport transport = new ResendMailTransport(restClientBuilder, "re_test_key", BASE_URL);

    @Test
    void anEmailIsPostedAsResendExpects() {
        resend.expect(requestTo("https://api.resend.test/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer re_test_key"))
                .andExpect(header(HttpHeaders.USER_AGENT, "issue-overflow"))
                .andExpect(jsonPath("$.from").value("Issue Overflow <support@issueoverflow.app>"))
                // element by element: value(List.of(...)) on the whole array read it as null
                .andExpect(jsonPath("$.to.length()").value(2))
                .andExpect(jsonPath("$.to[0]").value("ada@example.test"))
                .andExpect(jsonPath("$.to[1]").value("pat@example.test"))
                .andExpect(jsonPath("$.subject").value("Subject"))
                .andExpect(jsonPath("$.html").value("<p>Body</p>"))
                .andRespond(withSuccess(SENT, MediaType.APPLICATION_JSON));

        transport.send(email(List.of("ada@example.test", "pat@example.test")));
        resend.verify();
    }

    @Test
    void moreThanFiftyRecipientsGoOutAsSeveralEmails() {
        for(int size : new int[] {50, 50, 20})
            resend.expect(requestTo("https://api.resend.test/emails"))
                    .andExpect(jsonPath("$.to.length()").value(size))
                    .andRespond(withSuccess(SENT, MediaType.APPLICATION_JSON));

        transport.send(email(IntStream.range(0, 120).mapToObj(n -> "user" + n + "@example.test").toList()));
        resend.verify();
    }

    // EmailSender logs the exception; the transport must not swallow it
    @Test
    void aRefusedCallThrows() {
        resend.expect(requestTo("https://api.resend.test/emails"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> transport.send(email(List.of("ada@example.test"))))
                .isInstanceOf(RestClientResponseException.class);
    }

    private static Email email(List<String> to) {
        return new Email("support@issueoverflow.app", "Issue Overflow", to, "Subject", "<p>Body</p>");
    }
}
