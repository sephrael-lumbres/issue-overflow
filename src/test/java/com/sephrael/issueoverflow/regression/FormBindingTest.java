package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.Comment;
import com.sephrael.issueoverflow.entity.Issue;
import com.sephrael.issueoverflow.entity.Organization;
import com.sephrael.issueoverflow.entity.Project;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Forms bind to records that hold only the form's fields, so a request can't set anything else on an entity.
 * <p>
 * Binding a form straight to an entity (for example {@code User} or {@code Issue}) allowed two attacks, and the tests below send both:
 * <ul>
 *   <li>Setting properties the form never offers. Any request parameter that matched a property name was copied onto
 *   the entity, including {@code id} and {@code organization}.</li>
 *   <li>Overwriting an existing record. A request parameter named after the model attribute
 *   (for example {@code user=7} for a {@code User} parameter) made Spring load the record with that id from the
 *   database and bind the other form fields onto it. This allowed the request to edit a record it didn't create.</li>
 * </ul>
 * IDs that a form legitimately sends (the project, the assignee, the record being edited)
 * are looked up inside the caller's organization. Anything else is a 404.
 */
class FormBindingTest extends WebIntegrationTestBase {

    @Test
    void registrationCannotOverwriteAnExistingUser() throws Exception {
        User victim = createFixture().projectManager;
        String email = "new-" + uniqueSuffix().toLowerCase() + "@example.test";

        mockMvc.perform(post("/process_register").with(csrf())
                .param("user", victim.getId().toString())
                .param("id", victim.getId().toString())
                .param("firstName", "Mallory")
                .param("lastName", "Example")
                .param("email", email)
                .param("password", "another-password"));

        User reloadedVictim = userRepository.findUserById(victim.getId());
        assertThat(reloadedVictim.getEmail()).isEqualTo(victim.getEmail());
        assertThat(reloadedVictim.getOrganization()).isNotNull();

        User registeredUser = userRepository.findByEmail(email);
        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.getId()).isNotEqualTo(victim.getId());
    }

    @Test
    void registrationCannotJoinAnOrganization() throws Exception {
        Organization organization = createFixture().organization;
        String email = "new-" + uniqueSuffix().toLowerCase() + "@example.test";

        mockMvc.perform(post("/process_register").with(csrf())
                .param("organization", organization.getId().toString())
                .param("emailVerified", "true")
                .param("firstName", "Mallory")
                .param("lastName", "Example")
                .param("email", email)
                .param("password", "another-password"));

        User registeredUser = userRepository.findByEmail(email);
        assertThat(registeredUser.getOrganization()).isNull();
        assertThat(registeredUser.getRole().getName()).isEqualTo("Guest");
        assertThat(registeredUser.isEmailVerified()).isFalse();
    }

    @Test
    void profileFormCannotChangeAnotherUser() throws Exception {
        User victim = createFixture().projectManager;
        User attacker = createFixture().projectManager;
        MockHttpSession session = login(attacker.getEmail());

        mockMvc.perform(post("/account/profile/" + attacker.getId() + "/save").session(session).with(csrf())
                .param("user", victim.getId().toString())
                .param("firstName", "Changed")
                .param("lastName", "Changed")
                .param("email", attacker.getEmail()));

        User reloadedVictim = userRepository.findUserById(victim.getId());
        assertThat(reloadedVictim.getFirstName()).isEqualTo(victim.getFirstName());
        assertThat(reloadedVictim.getLastName()).isEqualTo(victim.getLastName());
    }

    @Test
    void organizationFormCannotTakeOverAnOrganization() throws Exception {
        Fixture victim = createFixture();
        User newUser = createUser(null, "Guest");
        MockHttpSession session = login(newUser.getEmail());

        mockMvc.perform(post("/organization/save").session(session).with(csrf())
                .param("organization", victim.organization.getId().toString())
                .param("id", victim.organization.getId().toString())
                .param("name", "Taken over"));

        assertThat(organizationRepository.findOrganizationById(victim.organization.getId()).getName())
                .isEqualTo(victim.organization.getName());

        User reloadedNewUser = userRepository.findByEmail(newUser.getEmail());
        assertThat(reloadedNewUser.getOrganization()).isNotNull();
        assertThat(reloadedNewUser.getOrganization().getId()).isNotEqualTo(victim.organization.getId());
    }

    @Test
    void commentFormCannotOverwriteAnotherComment() throws Exception {
        Fixture victim = createFixture();
        Comment victimComment = createComment(victim.issue, victim.projectManager, "Original comment");
        Fixture attacker = createFixture();
        MockHttpSession session = login(attacker.projectManager.getEmail());

        mockMvc.perform(post(issuePath(attacker) + "/comment/new").session(session).with(csrf())
                .param("comment", victimComment.getId().toString())
                .param("id", victimComment.getId().toString())
                .param("message", "Overwritten"));

        Comment reloadedComment = commentRepository.findCommentById(victimComment.getId());
        assertThat(reloadedComment.getMessage()).isEqualTo("Original comment");
        assertThat(reloadedComment.getIssue().getId()).isEqualTo(victim.issue.getId());
    }

    @Test
    void projectFormCannotTakeOverAProject() throws Exception {
        Fixture victim = createFixture();
        Fixture attacker = createFixture();
        MockHttpSession session = login(attacker.projectManager.getEmail());

        mockMvc.perform(post("/projects/new").session(session).with(csrf())
                .param("project", victim.project.getId().toString())
                .param("id", victim.project.getId().toString())
                .param("name", "Taken over")
                .param("identifier", "T" + uniqueSuffix())
                .param("color", "red"));

        Project reloadedProject = projectRepository.findProjectById(victim.project.getId());
        assertThat(reloadedProject.getName()).isEqualTo(victim.project.getName());
        assertThat(reloadedProject.getOrganization().getId()).isEqualTo(victim.organization.getId());
    }

    @Test
    void issueFormCannotOverwriteAnIssue() throws Exception {
        Fixture victim = createFixture();
        Fixture attacker = createFixture();

        // a project with no issues yet, so the new issue starts its key sequence
        Project emptyProject = createProject(attacker.organization, attacker.projectManager);
        MockHttpSession session = login(attacker.projectManager.getEmail());

        mockMvc.perform(newIssueRequest(emptyProject, "Overwritten").session(session).with(csrf())
                .param("issue", victim.issue.getId().toString())
                .param("id", victim.issue.getId().toString()));

        Issue reloadedIssue = issueRepository.findIssueById(victim.issue.getId());
        assertThat(reloadedIssue.getTitle()).isEqualTo(victim.issue.getTitle());
        assertThat(reloadedIssue.getOrganization().getId()).isEqualTo(victim.organization.getId());
    }

    @Test
    void userUpdateCannotChangeAnotherUser() throws Exception {
        User victim = createFixture().projectManager;
        Fixture attacker = createFixture();
        User developer = createUser(attacker.organization, "Developer");
        MockHttpSession session = login(attacker.projectManager.getEmail());

        mockMvc.perform(post("/users/update/" + developer.getId()).session(session).with(csrf())
                .param("role", roleRepository.findByName("Developer").getId().toString())
                .param("user", victim.getId().toString())
                .param("email", "changed-" + uniqueSuffix().toLowerCase() + "@example.test"));

        assertThat(userRepository.findUserById(victim.getId()).getEmail()).isEqualTo(victim.getEmail());
    }

    @Test
    void newIssueCannotUseAnotherOrganizationsProject() throws Exception {
        Fixture victim = createFixture();
        Fixture attacker = createFixture();
        MockHttpSession session = login(attacker.projectManager.getEmail());

        mockMvc.perform(newIssueRequest(victim.project, "Planted").session(session).with(csrf()))
                .andExpect(status().isNotFound());

        assertThat(issueRepository.findByProject(victim.project)).hasSize(1);
    }

    @Test
    void issueUpdateCannotTargetAnotherOrganizationsIssue() throws Exception {
        Fixture victim = createFixture();
        Fixture attacker = createFixture();
        MockHttpSession session = login(attacker.projectManager.getEmail());

        mockMvc.perform(post("/issues/update").session(session).with(csrf())
                        .param("id", victim.issue.getId().toString())
                        .param("title", "Overwritten")
                        .param("project", attacker.project.getId().toString()))
                .andExpect(status().isNotFound());

        assertThat(issueRepository.findIssueById(victim.issue.getId()).getTitle()).isEqualTo(victim.issue.getTitle());
    }

    @Test
    void issueUpdateCannotUseAnotherOrganizationsProjectOrUser() throws Exception {
        Fixture victim = createFixture();
        Fixture attacker = createFixture();
        MockHttpSession session = login(attacker.projectManager.getEmail());

        mockMvc.perform(post("/issues/update").session(session).with(csrf())
                        .param("id", attacker.issue.getId().toString())
                        .param("title", "Moved")
                        .param("project", victim.project.getId().toString()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/issues/update").session(session).with(csrf())
                        .param("id", attacker.issue.getId().toString())
                        .param("title", "Reassigned")
                        .param("project", attacker.project.getId().toString())
                        .param("assignedTo", victim.projectManager.getId().toString()))
                .andExpect(status().isNotFound());

        Issue reloadedIssue = issueRepository.findIssueById(attacker.issue.getId());
        assertThat(reloadedIssue.getTitle()).isEqualTo(attacker.issue.getTitle());
        assertThat(reloadedIssue.getProject().getId()).isEqualTo(attacker.project.getId());
        assertThat(reloadedIssue.getAssignedTo().getId()).isEqualTo(attacker.projectManager.getId());
    }

    // The status buttons and the edit form send " " for "unassigned".
    @Test
    void issueUpdateStillWorksWithinTheOrganization() throws Exception {
        Fixture fixture = createFixture();
        MockHttpSession session = login(fixture.projectManager.getEmail());

        mockMvc.perform(post("/issues/update").session(session).with(csrf())
                        .param("id", fixture.issue.getId().toString())
                        .param("title", "Updated title")
                        .param("type", "Task")
                        .param("priority", "Low")
                        .param("status", "Resolved")
                        .param("project", fixture.project.getId().toString())
                        .param("assignedTo", " "))
                .andExpect(status().is3xxRedirection());

        Issue reloadedIssue = issueRepository.findIssueById(fixture.issue.getId());
        assertThat(reloadedIssue.getTitle()).isEqualTo("Updated title");
        assertThat(reloadedIssue.getStatus()).isEqualTo("Resolved");
        assertThat(reloadedIssue.getAssignedTo()).isNull();
    }

    // The status buttons post the form the issue page renders, including the assignee as a hidden input.
    // Its value is the user's id (Spring Data's entity-to-id converter formats it), which IssueForm reads as a string.
    @Test
    void statusButtonsKeepTheAssignee() throws Exception {
        Fixture fixture = createFixture();
        MockHttpSession session = login(fixture.projectManager.getEmail());
        String issuePath = "/issues/" + fixture.project.getIdentifier() + "/view/" + fixture.issue.getIssueKey();

        String page = mockMvc.perform(get(issuePath).session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Matcher input = Pattern.compile("<input[^>]*id=\"issueAssignedTo\"[^>]*>").matcher(page);
        assertThat(input.find()).as("hidden assignee input on the issue page").isTrue();
        Matcher value = Pattern.compile("value=\"([^\"]*)\"").matcher(input.group());
        assertThat(value.find()).as("value of the hidden assignee input").isTrue();
        assertThat(value.group(1)).isEqualTo(fixture.projectManager.getId().toString());

        mockMvc.perform(post("/issues/update").session(session).with(csrf())
                        .param("id", fixture.issue.getId().toString())
                        .param("title", fixture.issue.getTitle())
                        .param("type", "Bug")
                        .param("priority", "High")
                        .param("status", "Resolved")
                        .param("project", fixture.project.getId().toString())
                        .param("assignedTo", value.group(1)))
                .andExpect(status().is3xxRedirection());

        Issue reloadedIssue = issueRepository.findIssueById(fixture.issue.getId());
        assertThat(reloadedIssue.getStatus()).isEqualTo("Resolved");
        assertThat(reloadedIssue.getAssignedTo().getId()).isEqualTo(fixture.projectManager.getId());
    }

    @Test
    void projectEditCannotTargetAnotherOrganizationsProject() throws Exception {
        Fixture victim = createFixture();
        Fixture attacker = createFixture();
        MockHttpSession session = login(attacker.projectManager.getEmail());

        mockMvc.perform(post("/projects/save").session(session).with(csrf())
                        .param("id", victim.project.getId().toString())
                        .param("name", "Renamed")
                        .param("identifier", victim.project.getIdentifier())
                        .param("color", "red"))
                .andExpect(status().isNotFound());

        assertThat(projectRepository.findProjectById(victim.project.getId()).getName()).isEqualTo(victim.project.getName());
    }

    @Test
    void projectEditKeepsTheProjectInItsOrganization() throws Exception {
        Fixture victim = createFixture();
        Fixture fixture = createFixture();
        MockHttpSession session = login(fixture.projectManager.getEmail());

        mockMvc.perform(post("/projects/save").session(session).with(csrf())
                        .param("id", fixture.project.getId().toString())
                        .param("organization", victim.organization.getId().toString())
                        .param("name", "Renamed")
                        .param("identifier", fixture.project.getIdentifier())
                        .param("color", "red"))
                .andExpect(status().is3xxRedirection());

        Project reloadedProject = projectRepository.findProjectById(fixture.project.getId());
        assertThat(reloadedProject.getName()).isEqualTo("Renamed");
        assertThat(reloadedProject.getOrganization().getId()).isEqualTo(fixture.organization.getId());
    }

    private static String issuePath(Fixture fixture) {
        return "/issues/" + fixture.project.getIdentifier() + "/view/" + fixture.issue.getIssueKey();
    }

    // The create form is multipart and always sends a "files" part.
    private static MockMultipartHttpServletRequestBuilder newIssueRequest(Project project, String title) {
        return multipart("/issues/new")
                .file(new MockMultipartFile("files", "", "application/octet-stream", new byte[0]))
                .param("title", title)
                .param("description", "Planted description")
                .param("type", "Bug")
                .param("priority", "High")
                .param("status", "Open")
                .param("project", project.getId().toString())
                .param("assignedTo", " ");
    }
}
