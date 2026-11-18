package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * The contact form is anonymous, and the sender's name goes into the email's subject: one line, at most 100 characters.
 */
class ContactFormSubjectTest extends WebIntegrationTestBase {

    @Test
    void theSubjectHoldsTheNameOnOneShortLine() throws Exception {
        mockMvc.perform(post("/contact-us/send-message").with(csrf())
                .param("name", "Mallory\r\nBcc: victim@example.test\n" + "x".repeat(200))
                .param("email", "mallory@example.test")
                .param("message", "Hello"));

        ArgumentCaptor<MimeMessage> message = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(message.capture());

        String subject = message.getValue().getSubject();
        assertThat(subject)
                .startsWith("Contact Us - Message from: Mallory  Bcc: victim@example.test")
                .doesNotContain("\r")
                .doesNotContain("\n");
        assertThat(subject.length()).isLessThanOrEqualTo("Contact Us - Message from: ".length() + 100);
    }
}
