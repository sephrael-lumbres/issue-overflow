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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A session keeps the authorities and password hash it was granted at login.
 * When a user's role, email or password changes, their other sessions are logged out at the next request.
 */
class StaleSessionTest extends WebIntegrationTestBase {

    @Test
    void loweringARoleLogsOutThatUsersSession() throws Exception {
        Fixture fixture = createFixture();
        User developer = createUser(fixture.organization, "Developer");
        MockHttpSession developerSession = login(developer.getEmail());
        MockHttpSession managerSession = login(fixture.projectManager.getEmail());
        String newIssuePage = "/issues/" + fixture.project.getIdentifier() + "/new";

        mockMvc.perform(get(newIssuePage).session(developerSession)).andExpect(status().isOk());

        mockMvc.perform(post("/users/update/" + developer.getId()).session(managerSession).with(csrf())
                        .param("role", roleRepository.findByName("Guest").getId().toString()))
                .andExpect(redirectedUrl("/users"));

        mockMvc.perform(get(newIssuePage).session(developerSession)).andExpect(redirectedUrl("/login?expired"));
        mockMvc.perform(get("/dashboard").session(developerSession)).andExpect(redirectedUrl("/login"));
    }

    @Test
    void changingYourEmailLogsOutYourOtherSessions() throws Exception {
        User user = createFixture().projectManager;
        MockHttpSession firstSession = login(user.getEmail());
        MockHttpSession secondSession = login(user.getEmail());

        mockMvc.perform(post("/account/profile/" + user.getId() + "/save").session(firstSession).with(csrf())
                        .param("firstName", user.getFirstName())
                        .param("lastName", user.getLastName())
                        .param("email", "changed-" + uniqueSuffix().toLowerCase() + "@example.test"))
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/dashboard").session(secondSession)).andExpect(redirectedUrl("/login?expired"));
    }

    @Test
    void changingYourPasswordLogsOutEverySession() throws Exception {
        User user = createFixture().projectManager;
        MockHttpSession firstSession = login(user.getEmail());
        MockHttpSession secondSession = login(user.getEmail());

        mockMvc.perform(post("/account/security/" + user.getId() + "/save").session(firstSession).with(csrf())
                        .param("currentPassword", PASSWORD)
                        .param("newPassword", "a-new-password")
                        .param("confirmPassword", "a-new-password"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("success", "Your password has been changed. Please log in with your new password."));

        mockMvc.perform(get("/dashboard").session(firstSession))
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/dashboard").session(secondSession))
                .andExpect(redirectedUrl("/login?expired"));
    }

    @Test
    void resettingYourPasswordLogsOutExistingSessions() throws Exception {
        User user = createFixture().projectManager;
        MockHttpSession session = login(user.getEmail());
        mockMvc.perform(post("/reset-password").with(csrf()).param("email", user.getEmail()));
        Matcher token = Pattern.compile("change-password\\?token=([A-Za-z0-9_-]{43})").matcher(sentEmailBody());
        assertThat(token.find()).as("token link in the email").isTrue();

        mockMvc.perform(post("/change-password").with(csrf())
                        .param("token", token.group(1))
                        .param("password", "a-new-password")
                        .param("confirmPassword", "a-new-password"))
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/dashboard").session(session)).andExpect(redirectedUrl("/login?expired"));
    }

    @Test
    void loginPageExplainsTheLogout() throws Exception {
        String page = mockMvc.perform(get("/login").param("expired", ""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(page).contains("Your role or account changed since you logged in. Please log in again.");
    }
}
