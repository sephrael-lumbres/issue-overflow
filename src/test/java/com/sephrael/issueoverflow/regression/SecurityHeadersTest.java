package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * The headers every page gets, and the error and cookie settings. MockMvc has no servlet container,
 * so the cookie flags are checked as settings; the live smoke test checks the Set-Cookie header itself.
 */
class SecurityHeadersTest extends WebIntegrationTestBase {

    @Autowired
    private Environment environment;

    @Test
    void pagesGetTheSecurityHeaders() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/login")).andReturn().getResponse();

        assertThat(response.getHeader("Content-Security-Policy"))
                .isEqualTo("object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'");
        assertThat(response.getHeader("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
    }

    // Spring Security writes HSTS only on requests it sees as HTTPS; on Railway that depends
    // on the prod profile's forwarded-header settings, which the live smoke test checks
    @Test
    void strictTransportSecurityIsSentOnlyOverHttps() throws Exception {
        assertThat(mockMvc.perform(get("/login").secure(true)).andReturn().getResponse()
                .getHeader("Strict-Transport-Security")).startsWith("max-age=31536000");
        assertThat(mockMvc.perform(get("/login")).andReturn().getResponse()
                .getHeader("Strict-Transport-Security")).isNull();
    }

    @Test
    void errorsHideTheirDetailsAndTheSessionCookieIsProtected() {
        assertThat(environment.getProperty("server.error.include-exception")).isEqualTo("false");
        assertThat(environment.getProperty("server.error.include-message")).isEqualTo("never");
        assertThat(environment.getProperty("server.error.include-binding-errors")).isEqualTo("never");
        assertThat(environment.getProperty("server.error.include-stacktrace")).isEqualTo("never");
        assertThat(environment.getProperty("server.servlet.session.cookie.http-only")).isEqualTo("true");
        assertThat(environment.getProperty("server.servlet.session.cookie.same-site")).isEqualTo("lax");
    }
}
