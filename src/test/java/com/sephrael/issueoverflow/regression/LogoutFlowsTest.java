package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.Organization;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Changing your own organization, role or email logs you out, so the next login picks up the new values.
 * Clearing SecurityContextHolder stopped counting as a logout in Spring Security 6; these flows call
 * SecurityContextLogoutHandler instead. Creating an organization is covered in SecurityRulesTest.
 */
class LogoutFlowsTest extends WebIntegrationTestBase {

    @Test
    void joiningAnEmptyOrganizationLogsTheUserOut() throws Exception {
        Organization empty = createOrganization();
        User newcomer = createUser(null, "Guest");
        MockHttpSession session = login(newcomer.getEmail());

        mockMvc.perform(post("/organization/joining").session(session).with(csrf()).param("accessKey", empty.getAccessKey()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(redirectedUrl("/login"));

        User reloaded = userRepository.findByEmail(newcomer.getEmail());
        assertThat(reloaded.getOrganization().getId()).isEqualTo(empty.getId());
        assertThat(reloaded.getRole().getName()).isEqualTo("Project Manager");
    }

    @Test
    void removingYourselfLogsYouOut() throws Exception {
        Fixture fixture = createFixture();
        createUser(fixture.organization, "Developer");
        MockHttpSession session = login(fixture.projectManager.getEmail());

        mockMvc.perform(post("/users/remove/" + fixture.projectManager.getId()).session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"));

        mockMvc.perform(get("/users").session(session))
                .andExpect(redirectedUrl("/login"));

        User reloaded = userRepository.findByEmail(fixture.projectManager.getEmail());
        assertThat(reloaded.getOrganization()).isNull();
        assertThat(reloaded.getRole().getName()).isEqualTo("Guest");
    }

    @Test
    void changingYourEmailLogsYouOutWithAMessage() throws Exception {
        Fixture fixture = createFixture();
        User user = fixture.projectManager;
        MockHttpSession session = login(user.getEmail());
        String newEmail = "changed-" + uniqueSuffix().toLowerCase() + "@example.test";
        String message = "Email address has been successfully updated. Please log in with your new email address.";

        MvcResult result = mockMvc.perform(post("/account/profile/" + user.getId() + "/save").session(session).with(csrf())
                        .param("firstName", user.getFirstName())
                        .param("lastName", user.getLastName())
                        .param("email", newEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("emailChangeSuccess", message))
                .andReturn();

        String loginPage = mockMvc.perform(get("/login").flashAttrs(result.getFlashMap()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(loginPage).contains(message);

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(redirectedUrl("/login"));
        assertThat(userRepository.findByEmail(newEmail)).isNotNull();
        assertThat(userRepository.findByEmail(user.getEmail())).isNull();
    }
}
