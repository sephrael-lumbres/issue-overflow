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

/**
 * Joining and creating organizations. Only a user without an organization can do either, so a member can't carry their
 * role into another organization. The first member of an organization becomes its Project Manager; later members join as Guests.
 */
class OrganizationJoinTest extends WebIntegrationTestBase {

    private static final String UNKNOWN_KEY_MESSAGE = "No organization matches that access code.";

    @Test
    void unknownAccessKeyShowsAnError() throws Exception {
        User newcomer = createUser(null, "Guest");
        MockHttpSession session = login(newcomer.getEmail());

        MvcResult result = mockMvc.perform(post("/organization/joining").session(session).with(csrf()).param("accessKey", "no-such-key"))
                .andExpect(redirectedUrl("/organization/join"))
                .andExpect(flash().attribute("joinOrganizationError", UNKNOWN_KEY_MESSAGE))
                .andReturn();

        String joinPage = mockMvc.perform(get("/organization/join").session(session).flashAttrs(result.getFlashMap()))
                .andReturn().getResponse().getContentAsString();
        assertThat(joinPage).contains(UNKNOWN_KEY_MESSAGE);
        assertThat(userRepository.findByEmail(newcomer.getEmail()).getOrganization()).isNull();
    }

    @Test
    void memberCannotJoinAnotherOrganization() throws Exception {
        Fixture own = createFixture();
        Fixture other = createFixture();
        MockHttpSession session = login(own.projectManager.getEmail());

        mockMvc.perform(post("/organization/joining").session(session).with(csrf())
                        .param("accessKey", other.organization.getAccessKey()))
                .andExpect(redirectedUrl("/organization"));

        User reloadedUser = userRepository.findByEmail(own.projectManager.getEmail());
        assertThat(reloadedUser.getOrganization().getId()).isEqualTo(own.organization.getId());
        assertThat(reloadedUser.getRole().getName()).isEqualTo("Project Manager");
    }

    @Test
    void memberCannotCreateAnotherOrganization() throws Exception {
        Fixture own = createFixture();
        MockHttpSession session = login(own.projectManager.getEmail());

        mockMvc.perform(post("/organization/save").session(session).with(csrf()).param("name", "Second " + uniqueSuffix()))
                .andExpect(redirectedUrl("/organization"));

        assertThat(userRepository.findByEmail(own.projectManager.getEmail()).getOrganization().getId())
                .isEqualTo(own.organization.getId());
    }

    @Test
    void joiningAnOrganizationWithMembersMakesYouAGuest() throws Exception {
        Organization organization = createFixture().organization;
        User newcomer = createUser(null, "Developer");
        MockHttpSession session = login(newcomer.getEmail());

        mockMvc.perform(post("/organization/joining").session(session).with(csrf()).param("accessKey", organization.getAccessKey()))
                .andExpect(redirectedUrl("/dashboard"));

        User reloadedUser = userRepository.findByEmail(newcomer.getEmail());
        assertThat(reloadedUser.getOrganization().getId()).isEqualTo(organization.getId());
        assertThat(reloadedUser.getRole().getName()).isEqualTo("Guest");
    }
}
