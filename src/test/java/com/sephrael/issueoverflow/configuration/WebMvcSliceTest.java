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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/**
 * A @WebMvcTest slice starts without a database. It failed at startup while @EnableJpaRepositories sat on the
 * application class, because the slice then tried to build every repository (no entityManagerFactory).
 * <p>
 * The slice skips @Configuration classes, so the security rules are imported;
 * their filter needs a UserRepository, which is mocked here like the controller's other collaborator.
 * The contact form redirects without rendering a page, so the test needs nothing from the templates.
 */
@WebMvcTest(HelpController.class)
@Import(WebSecurityConfig.class)
@ActiveProfiles("test")
class WebMvcSliceTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private EmailSender emailSender;

    @Test
    void aControllerSliceStartsWithoutTheDatabase() throws Exception {
        mockMvc.perform(post("/contact-us/send-message").with(csrf())
                        .param("name", "Ada")
                        .param("email", "ada@example.test")
                        .param("message", "Hello"))
                .andExpect(redirectedUrl("/contact-us"));

        verify(emailSender).send(any(Email.class));
    }
}
