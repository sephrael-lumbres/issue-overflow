package com.sephrael.issueoverflow.configuration;

import com.sephrael.issueoverflow.controller.HelpController;
import com.sephrael.issueoverflow.repository.UserRepository;
import com.sephrael.issueoverflow.service.mail.Email;
import com.sephrael.issueoverflow.service.mail.EmailSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The limits run inside the real security chain (WebSecurityConfig), turned on here with the production header.
 * A slice like WebMvcSliceTest, so it starts its own small context and doesn't change the full-stack tests' one.
 */
@WebMvcTest(controllers = HelpController.class, properties = {"app.rate-limit.enabled=true", "app.rate-limit.client-ip-header=X-Real-IP"})
@Import(WebSecurityConfig.class)
@ActiveProfiles("test")
class RateLimitWiringTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private EmailSender emailSender;

    @Test
    void theContactFormAnswers429AfterFivePostsFromOneAddress() throws Exception {
        for(int post = 0; post < 5; post++) {
            sendContactMessage("203.0.113.7").andExpect(redirectedUrl("/contact-us"));
        }

        sendContactMessage("203.0.113.7")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
        // another client keeps its own count
        sendContactMessage("203.0.113.8").andExpect(redirectedUrl("/contact-us"));

        verify(emailSender, times(6)).send(any(Email.class));
    }

    // 429 instead of the login redirect: the filter runs before Spring Security's login handling
    @Test
    void loginIsLimitedBeforeAuthenticationRuns() throws Exception {
        for(int attempt = 0; attempt < 10; attempt++) {
            logIn("limited@example.test", "198.51.100." + attempt).andExpect(status().is3xxRedirection());
        }

        logIn("LIMITED@example.test", "198.51.100.200").andExpect(status().isTooManyRequests());
    }

    private ResultActions sendContactMessage(String clientAddress) throws Exception {
        return mockMvc.perform(post("/contact-us/send-message").with(csrf())
                .header("X-Real-IP", clientAddress)
                .param("name", "Ada")
                .param("email", "ada@example.test")
                .param("message", "Hello"));
    }

    private ResultActions logIn(String email, String clientAddress) throws Exception {
        return mockMvc.perform(post("/login").with(csrf())
                .header("X-Real-IP", clientAddress)
                .param("email", email)
                .param("password", "not-the-password"));
    }
}
