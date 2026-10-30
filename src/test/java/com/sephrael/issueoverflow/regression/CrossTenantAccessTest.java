package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.Comment;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every route loads records within the caller's organization.
 * A record from another organization, or one that doesn't exist, shows the 404 page and changes nothing.
 */
class CrossTenantAccessTest extends WebIntegrationTestBase {

    @Test
    void organizationUpdateCannotRenameAnotherOrganization() throws Exception {
        Fixture victim = createFixture();
        Fixture attacker = createFixture();
        MockHttpSession session = login(attacker.projectManager.getEmail());

        mockMvc.perform(post("/organization/update").session(session).with(csrf())
                        .param("id", victim.organization.getId().toString())
                        .param("organizationName", "Renamed"))
                .andExpect(status().isNotFound());

        assertThat(organizationRepository.findOrganizationById(victim.organization.getId()).getName())
                .isEqualTo(victim.organization.getName());
    }

    @Test
    void commentEditCannotChangeAnotherOrganizationsComment() throws Exception {
        Fixture victim = createFixture();
        Comment victimComment = createComment(victim.issue, victim.projectManager, "Original comment");
        Fixture attacker = createFixture();
        MockHttpSession session = login(attacker.projectManager.getEmail());

        mockMvc.perform(post(issuePath(attacker) + "/comment/save").session(session).with(csrf())
                        .param("id", victimComment.getId().toString())
                        .param("isEdited", "false")
                        .param("message", "Overwritten"))
                .andExpect(status().isNotFound());

        assertThat(commentRepository.findCommentById(victimComment.getId()).getMessage()).isEqualTo("Original comment");
    }

    @Test
    void commentEditCannotChangeAnotherMembersComment() throws Exception {
        Fixture fixture = createFixture();
        Comment managerComment = createComment(fixture.issue, fixture.projectManager, "Original comment");
        User developer = createUser(fixture.organization, "Developer");
        fixture.project.addUser(developer);
        projectRepository.save(fixture.project);
        MockHttpSession session = login(developer.getEmail());

        mockMvc.perform(post(issuePath(fixture) + "/comment/save").session(session).with(csrf())
                        .param("id", managerComment.getId().toString())
                        .param("isEdited", "false")
                        .param("message", "Overwritten"))
                .andExpect(status().isNotFound());

        assertThat(commentRepository.findCommentById(managerComment.getId()).getMessage()).isEqualTo("Original comment");
    }

    @Test
    void commentDeleteCannotRemoveAnotherOrganizationsComment() throws Exception {
        Fixture victim = createFixture();
        Comment victimComment = createComment(victim.issue, victim.projectManager, "Original comment");
        Fixture attacker = createFixture();
        MockHttpSession session = login(attacker.projectManager.getEmail());

        mockMvc.perform(post(issuePath(attacker) + "/comment/" + victimComment.getId() + "/delete")
                        .session(session).with(csrf()))
                .andExpect(status().isNotFound());

        assertThat(commentRepository.findCommentById(victimComment.getId())).isNotNull();
    }

    @Test
    void userUpdateCannotAddAMemberToAnotherOrganizationsProject() throws Exception {
        Fixture victim = createFixture();
        Fixture attacker = createFixture();
        User developer = createUser(attacker.organization, "Developer");
        MockHttpSession session = login(attacker.projectManager.getEmail());

        mockMvc.perform(post("/users/update/" + developer.getId()).session(session).with(csrf())
                        .param("role", roleRepository.findByName("Developer").getId().toString())
                        .param("projects", victim.project.getId().toString()))
                .andExpect(status().isNotFound());

        inTransaction(() -> assertThat(projectRepository.findProjectById(victim.project.getId()).getUsers())
                .extracting(User::getId)
                .containsExactly(victim.projectManager.getId()));
    }

    @Test
    void attachmentDeleteWithAnUnknownKeyIsNotFound() throws Exception {
        Fixture fixture = createFixture();
        MockHttpSession session = login(fixture.projectManager.getEmail());

        mockMvc.perform(post("/storage/" + fixture.project.getIdentifier() + "/" + fixture.issue.getIssueKey()
                        + "/delete/no-such-key").session(session).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void issuePagesForUnknownOrMismatchedKeysAreNotFound() throws Exception {
        Fixture fixture = createFixture();
        Fixture otherOrganization = createFixture();
        MockHttpSession session = login(fixture.projectManager.getEmail());
        String projectIdentifier = fixture.project.getIdentifier();

        mockMvc.perform(get("/issues/" + projectIdentifier + "/view/" + projectIdentifier + "-999").session(session))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/issues/" + projectIdentifier + "/edit/" + projectIdentifier + "-999").session(session))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/issues/" + otherOrganization.project.getIdentifier() + "/view/"
                        + otherOrganization.issue.getIssueKey()).session(session))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/projects/" + otherOrganization.project.getIdentifier()).session(session))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/issues/" + otherOrganization.project.getIdentifier()).session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void accountPagesOfAnotherUserAreNotFound() throws Exception {
        Fixture fixture = createFixture();
        User otherUser = createUser(fixture.organization, "Developer");
        MockHttpSession session = login(fixture.projectManager.getEmail());

        mockMvc.perform(get("/account/profile/" + otherUser.getId()).session(session))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/account/profile/" + otherUser.getId() + "/save").session(session).with(csrf())
                        .param("firstName", "Changed")
                        .param("lastName", "Changed")
                        .param("email", otherUser.getEmail()))
                .andExpect(status().isNotFound());

        assertThat(userRepository.findUserById(otherUser.getId()).getFirstName()).isEqualTo(otherUser.getFirstName());
    }

    // The organization comes from the caller; the URL no longer names one.
    @Test
    void deletingYourAccountLeavesOtherOrganizationsAlone() throws Exception {
        Fixture victim = createFixture();
        Fixture own = createFixture();
        User developer = createUser(own.organization, "Developer");
        MockHttpSession session = login(developer.getEmail());

        mockMvc.perform(post("/account/security/" + developer.getId() + "/delete").session(session).with(csrf()).param("password", PASSWORD))
                .andExpect(redirectedUrl("/login"));

        assertThat(userRepository.findUserById(developer.getId())).isNull();
        assertThat(organizationRepository.findOrganizationById(own.organization.getId())).isNotNull();
        assertThat(organizationRepository.findOrganizationById(victim.organization.getId())).isNotNull();
        assertThat(userRepository.findUserById(victim.projectManager.getId()).getOrganization().getId())
                .isEqualTo(victim.organization.getId());
    }

    private static String issuePath(Fixture fixture) {
        return "/issues/" + fixture.project.getIdentifier() + "/view/" + fixture.issue.getIssueKey();
    }
}
