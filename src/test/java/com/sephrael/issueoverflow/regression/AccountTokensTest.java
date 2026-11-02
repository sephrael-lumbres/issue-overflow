package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.configuration.AppProperties;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.service.TokenGenerator;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Password reset and email verification tokens are stored as SHA-256 hashes, expire (app.password-reset-ttl,
 * app.email-verification-ttl), and work once. Each test reads the token from the one email it sends.
 */
class AccountTokensTest extends WebIntegrationTestBase {

    private static final Pattern RESET_TOKEN = Pattern.compile("change-password\\?token=([A-Za-z0-9_-]{43})");
    private static final Pattern VERIFICATION_TOKEN = Pattern.compile("emailVerificationToken=([A-Za-z0-9_-]{43})");

    @Autowired
    private AppProperties appProperties;

    @Test
    void resetTokenIsStoredAsAHashAndExpires() throws Exception {
        User user = createFixture().projectManager;
        LocalDateTime before = LocalDateTime.now();

        String token = requestPasswordReset(user);

        User reloadedUser = userRepository.findUserById(user.getId());
        assertThat(reloadedUser.getResetPasswordToken()).isEqualTo(TokenGenerator.hash(token)).isNotEqualTo(token);
        assertThat(reloadedUser.getResetPasswordTokenExpiresAt())
                .isAfter(before.plus(appProperties.passwordResetTtl()).minusMinutes(1))
                .isBefore(LocalDateTime.now().plus(appProperties.passwordResetTtl()).plusMinutes(1));
    }

    @Test
    void resetLinkWorksOnce() throws Exception {
        User user = createFixture().projectManager;
        String token = requestPasswordReset(user);

        mockMvc.perform(get("/change-password").param("token", token))
                .andExpect(status().isOk())
                .andExpect(view().name("change-password"));
        mockMvc.perform(post("/change-password").with(csrf())
                        .param("token", token)
                        .param("password", "a-new-password")
                        .param("confirmPassword", "a-new-password"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("success", "You have successfully changed your password!"));
        mockMvc.perform(post("/change-password").with(csrf())
                        .param("token", token)
                        .param("password", "another-password")
                        .param("confirmPassword", "another-password"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("error", "Invalid Token!"));
    }

    @Test
    void expiredResetLinkIsRejected() throws Exception {
        User user = createFixture().projectManager;
        String token = requestPasswordReset(user);
        User reloadedUser = userRepository.findUserById(user.getId());
        reloadedUser.setResetPasswordTokenExpiresAt(LocalDateTime.now().minusMinutes(1));
        userRepository.save(reloadedUser);

        mockMvc.perform(get("/change-password").param("token", token))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("error", "Invalid Token!"));
    }

    @Test
    void verificationLinkIsStoredAsAHashAndWorksOnce() throws Exception {
        User user = createFixture().projectManager;
        LocalDateTime before = LocalDateTime.now();

        String token = requestVerification(user);

        User reloadedUser = userRepository.findUserById(user.getId());
        assertThat(reloadedUser.getEmailVerificationToken()).isEqualTo(TokenGenerator.hash(token));
        assertThat(reloadedUser.getEmailVerificationTokenExpiresAt())
                .isAfter(before.plus(appProperties.emailVerificationTtl()).minusMinutes(1));

        mockMvc.perform(get("/verify-email").param("emailVerificationToken", token))
                .andExpect(model().attribute("userIsValid", true));
        assertThat(userRepository.findUserById(user.getId()).isEmailVerified()).isTrue();

        mockMvc.perform(get("/verify-email").param("emailVerificationToken", token))
                .andExpect(model().attribute("userIsNull", true));
    }

    @Test
    void expiredVerificationLinkIsRejected() throws Exception {
        User user = createFixture().projectManager;
        String token = requestVerification(user);
        User reloadedUser = userRepository.findUserById(user.getId());
        reloadedUser.setEmailVerificationTokenExpiresAt(LocalDateTime.now().minusMinutes(1));
        userRepository.save(reloadedUser);

        mockMvc.perform(get("/verify-email").param("emailVerificationToken", token))
                .andExpect(model().attribute("userIsNull", true));
        assertThat(userRepository.findUserById(user.getId()).isEmailVerified()).isFalse();
    }

    private String requestPasswordReset(User user) throws Exception {
        mockMvc.perform(post("/reset-password").with(csrf()).param("email", user.getEmail()));
        return tokenIn(sentEmailBody(), RESET_TOKEN);
    }

    private String requestVerification(User user) throws Exception {
        MockHttpSession session = login(user.getEmail());
        mockMvc.perform(post("/account/notifications/" + user.getId() + "/save").session(session).with(csrf())
                .param("created", "on"));
        return tokenIn(sentEmailBody(), VERIFICATION_TOKEN);
    }

    private static String tokenIn(String emailBody, Pattern pattern) {
        Matcher matcher = pattern.matcher(emailBody);
        assertThat(matcher.find()).as("token link in the email").isTrue();
        return matcher.group(1);
    }
}
