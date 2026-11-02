package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/**
 * Changing your password or email from the account pages cancels what the old credentials
 * allowed: a pending reset link, and the verified status of the old address.
 */
class AccountChangesTest extends WebIntegrationTestBase {

    private static final Pattern RESET_TOKEN = Pattern.compile("change-password\\?token=([A-Za-z0-9_-]{43})");

    @Test
    void changingYourEmailRequiresVerifyingItAgain() throws Exception {
        User user = userRepository.findUserById(createFixture().projectManager.getId());
        user.setEmailVerified(true);
        userRepository.save(user);
        MockHttpSession session = login(user.getEmail());
        String newEmail = "changed-" + uniqueSuffix().toLowerCase() + "@example.test";

        mockMvc.perform(post("/account/profile/" + user.getId() + "/save").session(session).with(csrf())
                .param("firstName", user.getFirstName())
                .param("lastName", user.getLastName())
                .param("email", newEmail));

        assertThat(userRepository.findByEmail(newEmail).isEmailVerified()).isFalse();
    }

    @Test
    void changingYourPasswordCancelsAResetLink() throws Exception {
        User user = createFixture().projectManager;
        mockMvc.perform(post("/reset-password").with(csrf()).param("email", user.getEmail()));
        Matcher matcher = RESET_TOKEN.matcher(sentEmailBody());
        assertThat(matcher.find()).as("token link in the email").isTrue();
        String token = matcher.group(1);
        MockHttpSession session = login(user.getEmail());

        mockMvc.perform(post("/account/security/" + user.getId() + "/save").session(session).with(csrf())
                .param("currentPassword", PASSWORD)
                .param("newPassword", "a-new-password")
                .param("confirmPassword", "a-new-password"));

        mockMvc.perform(get("/change-password").param("token", token))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("error", "Invalid Token!"));
    }
}
