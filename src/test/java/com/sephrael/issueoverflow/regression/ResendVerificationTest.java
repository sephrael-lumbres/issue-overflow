package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Resending the verification email takes POST (it changes the token and sends mail) and returns to the page it came from, never to another site.
 */
class ResendVerificationTest extends WebIntegrationTestBase {

    @Test
    void getNoLongerSendsAnEmail() throws Exception {
        User user = createFixture().projectManager;
        MockHttpSession session = login(user.getEmail());

        mockMvc.perform(get("/resend-verification-email").session(session)).andExpect(status().isMethodNotAllowed());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void returnsToThePageItCameFrom() throws Exception {
        User user = createFixture().projectManager;
        MockHttpSession session = login(user.getEmail());

        mockMvc.perform(post("/resend-verification-email").session(session).with(csrf()).header("Referer", "http://localhost/projects/all"))
                .andExpect(redirectedUrl("/projects/all"));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void aRefererOnAnotherSiteKeepsOnlyItsPath() throws Exception {
        User user = createFixture().projectManager;
        MockHttpSession session = login(user.getEmail());

        mockMvc.perform(post("/resend-verification-email").session(session).with(csrf()).header("Referer", "https://evil.example/phishing"))
                .andExpect(redirectedUrl("/phishing"));
        mockMvc.perform(post("/resend-verification-email").session(session).with(csrf()).header("Referer", "https://localhost//evil.example/phishing"))
                .andExpect(redirectedUrl("/dashboard"));
    }
}
