package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import jakarta.mail.Address;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/**
 * Email addresses are compared without case: Alice@Example.test and alice@example.test are one mailbox, so they are
 * one account. Addresses are kept in lower case, and every form that takes an address finds it in any case.
 */
class EmailCaseTest extends WebIntegrationTestBase {

    private static final String TAKEN = "An account with this email address already exists. Log in, or reset your password if you've forgotten it.";

    @Test
    void aDifferentlyCasedAddressCantRegisterASecondAccount() throws Exception {
        String address = "case-" + uniqueSuffix().toLowerCase(Locale.ROOT) + "@example.test";
        register(address.toUpperCase(Locale.ROOT)).andExpect(redirectedUrl("/login"));

        register(address)
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attribute("registrationError", TAKEN));
    }

    @Test
    void registrationKeepsTheAddressInLowerCase() throws Exception {
        String address = "Mixed-" + uniqueSuffix() + "@Example.test";

        register(address).andExpect(redirectedUrl("/login"));

        assertThat(userRepository.findByEmail(address.toLowerCase(Locale.ROOT))).isNotNull();
    }

    @Test
    void loginIgnoresTheCaseOfTheAddress() throws Exception {
        User user = createFixture().projectManager;

        mockMvc.perform(formLogin("/login").userParameter("email").user(user.getEmail().toUpperCase(Locale.ROOT)).password(PASSWORD))
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void passwordResetIgnoresTheCaseOfTheAddress() throws Exception {
        User user = createFixture().projectManager;

        mockMvc.perform(post("/reset-password").with(csrf()).param("email", user.getEmail().toUpperCase(Locale.ROOT)));

        ArgumentCaptor<MimeMessage> message = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(message.capture());

        assertThat(message.getValue().getAllRecipients()).extracting(Address::toString).containsExactly(user.getEmail());
    }

    @Test
    void aChangedAddressIsKeptInLowerCase() throws Exception {
        User user = createFixture().projectManager;
        MockHttpSession session = login(user.getEmail());
        String newAddress = "Changed-" + uniqueSuffix() + "@Example.test";

        mockMvc.perform(post("/account/profile/" + user.getId() + "/save").session(session).with(csrf())
                .param("firstName", user.getFirstName())
                .param("lastName", user.getLastName())
                .param("email", newAddress));

        assertThat(userRepository.findByEmail(newAddress.toLowerCase(Locale.ROOT))).isNotNull();
    }

    private ResultActions register(String email) throws Exception {
        return mockMvc.perform(post("/process_register").with(csrf())
                .param("firstName", "Ada")
                .param("lastName", "Lovelace")
                .param("email", email)
                .param("password", "a-long-enough-password"));
    }
}
