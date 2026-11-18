package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.Issue;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The create and edit forms keep the rules their pages show: only the author, a Project
 * Manager or an Admin can edit an issue, and only Project Managers and Admins can close one.
 * The pages used to hide the controls while the posts accepted them from any Developer member.
 */
class IssueFormRulesTest extends WebIntegrationTestBase {

    @Test
    void aDeveloperCannotEditAnotherMembersIssue() throws Exception {
        Fixture fixture = createFixture();
        MockHttpSession session = login(projectDeveloper(fixture).getEmail());

        mockMvc.perform(editForm(fixture, fixture.issue, "Rewritten by a developer", "Open").session(session))
                .andExpect(status().isForbidden());

        Issue reloadedIssue = issueRepository.findIssueById(fixture.issue.getId());
        assertThat(reloadedIssue.getTitle()).isEqualTo(fixture.issue.getTitle());
        assertThat(reloadedIssue.getStatus()).isEqualTo("Open");
    }

    @Test
    void aDeveloperCannotCloseTheirOwnIssueThroughTheEditForm() throws Exception {
        Fixture fixture = createFixture();
        User developer = projectDeveloper(fixture);
        Issue ownIssue = createIssue(fixture.project, developer, null, "Bug", "Open", "Low");
        MockHttpSession session = login(developer.getEmail());

        mockMvc.perform(editForm(fixture, ownIssue, ownIssue.getTitle(), "Closed").session(session))
                .andExpect(status().isForbidden());

        assertThat(issueRepository.findIssueById(ownIssue.getId()).getStatus()).isEqualTo("Open");
    }

    @Test
    void aDeveloperCannotCreateAClosedIssue() throws Exception {
        Fixture fixture = createFixture();
        MockHttpSession session = login(projectDeveloper(fixture).getEmail());

        mockMvc.perform(multipart("/issues/new").file(new MockMultipartFile("files", "", "application/octet-stream", new byte[0]))
                        .session(session).with(csrf())
                        .param("title", "Created closed")
                        .param("status", "Closed")
                        .param("project", fixture.project.getId().toString())
                        .param("assignedTo", " "))
                .andExpect(status().isForbidden());

        assertThat(issueRepository.findByProject(fixture.project)).hasSize(1);
    }

    @Test
    void aDeveloperCanStillEditAndResolveTheirOwnIssue() throws Exception {
        Fixture fixture = createFixture();
        User developer = projectDeveloper(fixture);
        Issue ownIssue = createIssue(fixture.project, developer, null, "Bug", "Open", "Low");
        MockHttpSession session = login(developer.getEmail());

        mockMvc.perform(editForm(fixture, ownIssue, "Edited by its author", "Resolved").session(session))
                .andExpect(status().is3xxRedirection());

        Issue reloadedIssue = issueRepository.findIssueById(ownIssue.getId());
        assertThat(reloadedIssue.getTitle()).isEqualTo("Edited by its author");
        assertThat(reloadedIssue.getStatus()).isEqualTo("Resolved");
    }

    // the issue page offers Attach File(s) to every Developer member, not only the author
    @Test
    void aDeveloperCanStillAttachFilesToAnotherMembersIssue() throws Exception {
        Fixture fixture = createFixture();
        MockHttpSession session = login(projectDeveloper(fixture).getEmail());

        mockMvc.perform(multipart("/issues/update")
                        .file(new MockMultipartFile("files", "notes.txt", "text/plain", "notes".getBytes(StandardCharsets.UTF_8)))
                        .session(session).with(csrf())
                        .param("id", fixture.issue.getId().toString())
                        .param("isAttachFileForm", "true"))
                .andExpect(status().is3xxRedirection());

        assertThat(awsFileRepository.findAll()).anyMatch(file ->
                file.getIssue() != null && file.getIssue().getId().equals(fixture.issue.getId()));
    }

    private User projectDeveloper(Fixture fixture) {
        User developer = createUser(fixture.organization, "Developer");
        fixture.project.addUser(developer);
        projectRepository.save(fixture.project);
        return developer;
    }

    private static MockHttpServletRequestBuilder editForm(Fixture fixture, Issue issue, String title, String status) {
        return post("/issues/update").with(csrf())
                .param("id", issue.getId().toString())
                .param("title", title)
                .param("type", "Bug")
                .param("priority", "Low")
                .param("status", status)
                .param("project", fixture.project.getId().toString())
                .param("assignedTo", " ");
    }
}
