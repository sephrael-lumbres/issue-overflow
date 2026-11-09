package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.AWSFile;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A project's issues, pages and attachments are for its members.
 * To other members of the organization they look missing (404), as the comment routes already did.
 */
class ProjectMembershipTest extends WebIntegrationTestBase {

    @Test
    void nonMembersCannotOpenTheProjectsPages() throws Exception {
        Fixture fixture = createFixture();
        User developer = createUser(fixture.organization, "Developer");
        MockHttpSession session = login(developer.getEmail());

        for (String path : projectPages(fixture)) {
            mockMvc.perform(get(path).session(session))
                    .andExpect(status().isNotFound());
        }

        mockMvc.perform(get("/issues/" + fixture.project.getIdentifier() + "/edit/" + fixture.issue.getIssueKey()).session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonMembersCannotCreateOrChangeTheProjectsIssues() throws Exception {
        Fixture fixture = createFixture();
        User developer = createUser(fixture.organization, "Developer");
        MockHttpSession session = login(developer.getEmail());

        mockMvc.perform(multipart("/issues/new").file(new MockMultipartFile("files", "", "application/octet-stream", new byte[0]))
                        .session(session).with(csrf())
                        .param("title", "Created by a non-member")
                        .param("project", fixture.project.getId().toString())
                        .param("assignedTo", " "))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/issues/update").session(session).with(csrf())
                        .param("id", fixture.issue.getId().toString())
                        .param("title", "Changed by a non-member")
                        .param("project", fixture.project.getId().toString())
                        .param("assignedTo", " "))
                .andExpect(status().isNotFound());

        assertThat(issueRepository.findByProject(fixture.project)).hasSize(1);
        assertThat(issueRepository.findIssueById(fixture.issue.getId()).getTitle()).isEqualTo(fixture.issue.getTitle());
    }

    @Test
    void nonMembersCannotDownloadTheProjectsAttachments() throws Exception {
        Fixture fixture = createFixture();
        AWSFile attachment = uploadAttachment(fixture, "notes.txt", "text/plain", "notes".getBytes(StandardCharsets.UTF_8));
        User developer = createUser(fixture.organization, "Developer");
        MockHttpSession session = login(developer.getEmail());

        mockMvc.perform(get("/storage/files/" + attachment.getFileKey()).session(session))
                .andExpect(status().isNotFound());
    }

    // The issue is looked up before the comment is checked, so an empty comment from a non-member is a 404 too.
    @Test
    void nonMembersGetA404ForAnInvalidComment() throws Exception {
        Fixture fixture = createFixture();
        User developer = createUser(fixture.organization, "Developer");
        MockHttpSession session = login(developer.getEmail());
        String issuePath = "/issues/" + fixture.project.getIdentifier() + "/view/" + fixture.issue.getIssueKey();

        mockMvc.perform(post(issuePath + "/comment/new").session(session).with(csrf()).param("message", ""))
                .andExpect(status().isNotFound());
    }

    @Test
    void membersSeeTheProjectsPagesAndAttachments() throws Exception {
        Fixture fixture = createFixture();
        AWSFile attachment = uploadAttachment(fixture, "notes.txt", "text/plain", "notes".getBytes(StandardCharsets.UTF_8));
        User developer = createUser(fixture.organization, "Developer");
        fixture.project.addUser(developer);
        projectRepository.save(fixture.project);
        MockHttpSession session = login(developer.getEmail());

        for (String path : projectPages(fixture)) {
            mockMvc.perform(get(path).session(session))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/storage/files/" + attachment.getFileKey()).session(session))
                .andExpect(status().isFound());
    }

    private static List<String> projectPages(Fixture fixture) {
        String identifier = fixture.project.getIdentifier();
        return List.of(
                "/projects/" + identifier,
                "/issues/" + identifier,
                "/issues/" + identifier + "/results?status=Open",
                "/issues/" + identifier + "/new",
                "/issues/" + identifier + "/view/" + fixture.issue.getIssueKey());
    }
}
