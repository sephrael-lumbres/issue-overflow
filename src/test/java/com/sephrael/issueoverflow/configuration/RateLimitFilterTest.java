package com.sephrael.issueoverflow.configuration;

import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The filter on its own, with no Spring context. RateLimitWiringTest checks that the security chain runs it.
 */
class RateLimitFilterTest {

    private static final RateLimitSettings NO_HEADER = new RateLimitSettings(true, "");

    private final RateLimitFilter filter = new RateLimitFilter(NO_HEADER, Ticker.systemTicker());

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // Ten tries for one address, typed in any case and from any number of addresses, then 429.
    @Test
    void loginIsCountedPerEmailAddressWhateverItsCase() throws Exception {
        for(int attempt = 0; attempt < 10; attempt++) {
            String remoteAddr = "198.51.100." + attempt;
            String email = attempt % 2 == 0 ? "Ada@Example.test" : "ada@example.test ";
            assertThat(post("/login", request -> {
                request.setRemoteAddr(remoteAddr);
                request.addParameter("email", email);
            }).getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse limited = post("/login", request -> {
            request.setRemoteAddr("198.51.100.200");
            request.addParameter("email", "ADA@EXAMPLE.TEST");
        });

        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(limited.getHeader("Retry-After")).isNotBlank();
    }

    // An address over 255 characters can't be an account's, so it isn't stored or counted; the IP limit still applies.
    @Test
    void anOverlongEmailAddressIsNotCounted() throws Exception {
        String overlong = "a".repeat(250) + "@example.test";
        for(int attempt = 0; attempt < 15; attempt++) {
            String remoteAddr = "198.51.100." + attempt;
            assertThat(post("/login", request -> {
                request.setRemoteAddr(remoteAddr);
                request.addParameter("email", overlong);
            }).getStatus()).isEqualTo(200);
        }
    }

    // A post over the IP limit is answered before the email limit sees it, so its address isn't counted.
    @Test
    void aPostOverOneLimitCountsTowardNoOther() throws Exception {
        for(int attempt = 0; attempt < 20; attempt++) {
            String email = "user" + attempt + "@example.test";
            assertThat(post("/login", request -> request.addParameter("email", email)).getStatus()).isEqualTo(200);
        }
        assertThat(post("/login", request -> request.addParameter("email", "kept@example.test")).getStatus()).isEqualTo(429);

        // from other addresses, kept@example.test still has all ten tries
        for(int attempt = 0; attempt < 10; attempt++) {
            String remoteAddr = "198.51.100." + attempt;
            assertThat(post("/login", request -> {
                request.setRemoteAddr(remoteAddr);
                request.addParameter("email", "kept@example.test");
            }).getStatus()).isEqualTo(200);
        }
        assertThat(post("/login", request -> {
            request.setRemoteAddr("198.51.100.200");
            request.addParameter("email", "kept@example.test");
        }).getStatus()).isEqualTo(429);
    }

    // With no header configured, X-Real-IP and X-Forwarded-For are a client's own words: the connection's address counts.
    @Test
    void forwardedHeadersAreIgnoredUnlessConfigured() throws Exception {
        for(int attempt = 0; attempt < 5; attempt++) {
            String forgedAddress = "203.0.113." + attempt;
            assertThat(post("/contact-us/send-message", request -> {
                request.addHeader("X-Real-IP", forgedAddress);
                request.addHeader("X-Forwarded-For", forgedAddress);
            }).getStatus()).isEqualTo(200);
        }

        assertThat(post("/contact-us/send-message", request -> request.addHeader("X-Real-IP", "203.0.113.99")).getStatus())
                .isEqualTo(429);
    }

    @Test
    void accountFormsAreCountedPerSignedInUser() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("pat@example.test", null, List.of()));
        for(int attempt = 0; attempt < 3; attempt++) {
            assertThat(post("/resend-verification-email", request -> { }).getStatus()).isEqualTo(200);
        }

        assertThat(post("/resend-verification-email", request -> { }).getStatus()).isEqualTo(429);

        // nobody signed in: no account to count (security sends the visitor to the login page)
        SecurityContextHolder.clearContext();
        assertThat(post("/resend-verification-email", request -> { }).getStatus()).isEqualTo(200);
    }

    @Test
    void otherRequestsAreNotCounted() throws Exception {
        for(int attempt = 0; attempt < 50; attempt++) {
            assertThat(send(new MockHttpServletRequest("GET", "/login")).getStatus()).isEqualTo(200);
            assertThat(post("/issues/new", request -> { }).getStatus()).isEqualTo(200);
        }
    }

    private MockHttpServletResponse post(String path, Consumer<MockHttpServletRequest> customizer) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        customizer.accept(request);
        return send(request);
    }

    // 200 when the request reached the rest of the chain, 429 when the filter answered it
    private MockHttpServletResponse send(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
