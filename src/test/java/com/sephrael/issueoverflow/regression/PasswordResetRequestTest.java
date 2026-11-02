package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * The password reset form answers the same way whether or not the address has an account, and whether or not the email could be sent.
 */
class PasswordResetRequestTest extends WebIntegrationTestBase {

    private static final String MESSAGE = "If an account exists for that email address, we've sent it a link to reset the password.";

    @Test
    void registeredAndUnknownAddressesGetTheSameAnswer() throws Exception {
        String registeredEmail = createFixture().projectManager.getEmail();
        String unknownEmail = "nobody-" + uniqueSuffix().toLowerCase() + "@example.test";

        String registeredPage = requestReset(registeredEmail);
        String unknownPage = requestReset(unknownEmail);

        assertThat(registeredPage).contains(MESSAGE).doesNotContain("No account exists");
        assertThat(unknownPage).contains(MESSAGE).doesNotContain("No account exists");
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void aFailedSendGetsTheSameAnswer() throws Exception {
        doThrow(new MailSendException("SMTP server unavailable")).when(mailSender).send(any(MimeMessage.class));

        String page = requestReset(createFixture().projectManager.getEmail());

        assertThat(page).contains(MESSAGE);
    }

    private String requestReset(String email) throws Exception {
        return mockMvc.perform(post("/reset-password").with(csrf()).param("email", email))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"))
                .andExpect(model().attribute("resetRequested", true))
                .andReturn().getResponse().getContentAsString();
    }
}
