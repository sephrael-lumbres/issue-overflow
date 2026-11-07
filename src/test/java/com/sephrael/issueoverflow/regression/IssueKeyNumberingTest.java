package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.Issue;
import com.sephrael.issueoverflow.entity.Organization;
import com.sephrael.issueoverflow.entity.Project;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Issue keys are numbered per project from a counter that only goes up, and are unique within a project.
 */
class IssueKeyNumberingTest extends WebIntegrationTestBase {

    // The old code moved the project's key sequence on a rename and read it without a null check.
    @Test
    void theIdentifierOfAProjectWithoutIssuesCanBeChanged() throws Exception {
        Organization organization = createOrganization();
        User projectManager = createUser(organization, "Project Manager");
        Project project = createProject(organization, projectManager);
        String newIdentifier = "R" + uniqueSuffix();
        MockHttpSession session = login(projectManager.getEmail());

        mockMvc.perform(post("/projects/save").session(session).with(csrf())
                        .param("id", project.getId().toString())
                        .param("name", project.getName())
                        .param("identifier", newIdentifier)
                        .param("color", project.getColor()))
                .andExpect(redirectedUrl("/projects/all"));

        assertThat(projectRepository.findProjectById(project.getId()).getIdentifier()).isEqualTo(newIdentifier);
    }

    // After a rename, the old identifier is free for a new project.
    // Both projects then have an issue keyed "<old identifier>-1", and each page shows its own.
    @Test
    void aReusedIdentifierGetsItsOwnKeys() throws Exception {
        Fixture fixture = createFixture();
        String oldIdentifier = fixture.project.getIdentifier();
        String renamedIdentifier = "R" + uniqueSuffix();
        MockHttpSession session = login(fixture.projectManager.getEmail());

        mockMvc.perform(post("/projects/save").session(session).with(csrf())
                        .param("id", fixture.project.getId().toString())
                        .param("name", fixture.project.getName())
                        .param("identifier", renamedIdentifier)
                        .param("color", fixture.project.getColor()))
                .andExpect(redirectedUrl("/projects/all"));
        mockMvc.perform(post("/projects/new").session(session).with(csrf())
                        .param("name", "Second " + uniqueSuffix())
                        .param("identifier", oldIdentifier)
                        .param("color", "primary"))
                .andExpect(redirectedUrl("/projects/all"));
        Project secondProject = projectRepository.findByIdentifierAndOrganization(oldIdentifier, fixture.organization);
        String title = "Created after the rename " + uniqueSuffix();

        createIssueThroughForm(session, secondProject, title)
                .andExpect(redirectedUrl("/issues/" + oldIdentifier + "/view/" + oldIdentifier + "-1"));

        assertThat(fetchPage("/issues/" + oldIdentifier + "/view/" + oldIdentifier + "-1", session)).contains(title);
        assertThat(fetchPage("/issues/" + renamedIdentifier + "/view/" + fixture.issue.getIssueKey(), session))
                .contains(fixture.issue.getTitle());
    }

    @Test
    void numbersOfDeletedIssuesAreNotReused() throws Exception {
        Organization organization = createOrganization();
        User projectManager = createUser(organization, "Project Manager");
        Project project = createProject(organization, projectManager);
        String identifier = project.getIdentifier();
        MockHttpSession session = login(projectManager.getEmail());

        createIssueThroughForm(session, project, "First " + uniqueSuffix())
                .andExpect(redirectedUrl("/issues/" + identifier + "/view/" + identifier + "-1"));
        mockMvc.perform(post("/issues/" + identifier + "/delete/" + identifier + "-1/false").session(session).with(csrf()))
                .andExpect(redirectedUrl("/issues/" + identifier));
        createIssueThroughForm(session, project, "Second " + uniqueSuffix())
                .andExpect(redirectedUrl("/issues/" + identifier + "/view/" + identifier + "-2"));
    }

    @Test
    void aProjectCannotHoldTwoIssuesWithTheSameKey() {
        Fixture fixture = createFixture();
        Issue duplicate = new Issue();
        duplicate.setTitle("Same key " + uniqueSuffix());
        duplicate.setDescription("Saved with the key of the fixture's issue.");
        duplicate.setType("Task");
        duplicate.setStatus("Open");
        duplicate.setPriority("Low");
        duplicate.setIssueKey(fixture.issue.getIssueKey());
        duplicate.setProject(fixture.project);
        duplicate.setUser(fixture.projectManager);
        duplicate.setOrganization(fixture.organization);

        assertThatThrownBy(() -> issueRepository.save(duplicate))
                .hasStackTraceContaining("issue_project_id_issue_key_key");
        assertThat(issueRepository.findByProject(fixture.project)).extracting(Issue::getId).containsExactly(fixture.issue.getId());
    }

    private ResultActions createIssueThroughForm(MockHttpSession session, Project project, String title) throws Exception {
        return mockMvc.perform(multipart("/issues/new").file(new MockMultipartFile("files", "", "application/octet-stream", new byte[0]))
                .session(session).with(csrf())
                .param("title", title)
                .param("type", "Bug")
                .param("priority", "High")
                .param("status", "Open")
                .param("project", project.getId().toString())
                .param("assignedTo", " "));
    }

    private String fetchPage(String path, MockHttpSession session) throws Exception {
        return mockMvc.perform(get(path).session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
