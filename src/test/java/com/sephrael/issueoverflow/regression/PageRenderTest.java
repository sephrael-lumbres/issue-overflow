package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.Issue;
import com.sephrael.issueoverflow.entity.Organization;
import com.sephrael.issueoverflow.entity.Project;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class PageRenderTest extends WebIntegrationTestBase {

    // Renders every main page, including the Thymeleaf expressions that call
    // java.time methods and repository proxies from templates.
    @Test
    void mainPagesRenderForProjectManager() throws Exception {
        Fixture fixture = createFixture();
        String editedTitle = "Edited title " + uniqueSuffix();
        editIssueTitle(fixture.issue.getId(), editedTitle, fixture.projectManager);

        MockHttpSession session = login(fixture.projectManager.getEmail());
        String identifier = fixture.project.getIdentifier();
        String issueKey = fixture.issue.getIssueKey();
        Long userId = fixture.projectManager.getId();

        Map<String, String> expectedViews = new LinkedHashMap<>();
        expectedViews.put("/dashboard", "dashboard");
        expectedViews.put("/projects/all", "projects/projects-list");
        expectedViews.put("/projects/" + identifier, "projects/project-home");
        expectedViews.put("/issues/all", "issues/issues-by-organization");
        expectedViews.put("/issues/all/results?assignedTo=Unassigned&identifier=" + identifier, "issues/issues-by-organization");
        expectedViews.put("/issues/" + identifier, "issues/issues-by-project");
        expectedViews.put("/issues/" + identifier + "/results?status=Open", "issues/issues-by-project");
        expectedViews.put("/issues/" + identifier + "/view/" + issueKey, "issues/issue-details");
        expectedViews.put("/issues/" + identifier + "/new", "issues/create-issue");
        expectedViews.put("/issues/" + identifier + "/edit/" + issueKey, "issues/edit-issue");
        expectedViews.put("/users", "users/users-by-organization");
        expectedViews.put("/users/edit/" + userId, "users/edit-user");
        expectedViews.put("/organization", "organization/organization-details");
        expectedViews.put("/account/profile/" + userId, "account-settings/account-profile");
        expectedViews.put("/account/security/" + userId, "account-settings/account-security");
        expectedViews.put("/account/notifications/" + userId, "account-settings/account-notifications");

        for (Map.Entry<String, String> page : expectedViews.entrySet()) {
            MvcResult result = mockMvc.perform(get(page.getKey()).session(session)).andReturn();
            assertThat(result.getResponse().getStatus())
                    .as("HTTP status of GET %s", page.getKey()).isEqualTo(200);
            assertThat(result.getModelAndView())
                    .as("model and view of GET %s", page.getKey()).isNotNull();
            assertThat(result.getModelAndView().getViewName())
                    .as("view of GET %s", page.getKey()).isEqualTo(page.getValue());
        }

        String issuePage = mockMvc.perform(get("/issues/" + identifier + "/view/" + issueKey).session(session))
                .andReturn().getResponse().getContentAsString();
        assertThat(issuePage).contains(editedTitle);
        // The original title now appears only as the old value in the change history.
        assertThat(issuePage).contains(fixture.issue.getTitle());
    }

    // Creates an issue through the form endpoint, which also sets the issue key and starts the project's key sequence, then renders it.
    @Test
    void newIssueFormCreatesAndShowsTheIssue() throws Exception {
        Organization organization = createOrganization();
        User projectManager = createUser(organization, "Project Manager");
        Project project = createProject(organization, projectManager);
        MockHttpSession session = login(projectManager.getEmail());
        String title = "Created through the form " + uniqueSuffix();
        String expectedKey = project.getIdentifier() + "-1";
        LocalDate dueDate = LocalDate.now().plusDays(3);

        mockMvc.perform(multipart("/issues/new").file(emptyFilesPart()).session(session).with(csrf())
                        .param("title", title)
                        .param("description", "Steps to reproduce: open the dashboard.")
                        .param("type", "Bug")
                        .param("priority", "High")
                        .param("status", "Open")
                        .param("project", project.getId().toString())
                        .param("assignedTo", projectManager.getId().toString())
                        .param("estimatedHours", "2")
                        .param("dueDate", dueDate.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/issues/" + project.getIdentifier() + "/view/" + expectedKey));

        Issue createdIssue = issueRepository.findByIssueKeyAndOrganization(expectedKey, organization);
        assertThat(createdIssue).isNotNull();
        assertThat(createdIssue.getTitle()).isEqualTo(title);
        assertThat(createdIssue.getUser().getId()).isEqualTo(projectManager.getId());
        assertThat(createdIssue.getAssignedTo().getId()).isEqualTo(projectManager.getId());
        assertThat(createdIssue.getEstimatedHours()).isEqualTo(2);
        assertThat(createdIssue.getDueDate()).isEqualTo(dueDate);

        String issuePage = mockMvc.perform(get("/issues/" + project.getIdentifier() + "/view/" + expectedKey).session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(issuePage).contains(title);
    }

    // sec:authorize hides role-restricted controls: a Guest sees neither the close button nor the attach button, nor the user edit links.
    @Test
    void roleRestrictedControlsShowForProjectManagerOnly() throws Exception {
        Fixture fixture = createFixture();
        User guest = createUser(fixture.organization, "Guest");
        String issuePath = "/issues/" + fixture.project.getIdentifier() + "/view/" + fixture.issue.getIssueKey();
        String editLink = "/users/edit/" + guest.getId();

        MockHttpSession managerSession = login(fixture.projectManager.getEmail());
        assertThat(fetchPage(issuePath, managerSession))
                .contains("Mark as Closed")
                .contains("data-bs-target=\"#attachFileModal\"");
        assertThat(fetchPage("/users", managerSession)).contains(editLink);

        MockHttpSession guestSession = login(guest.getEmail());
        assertThat(fetchPage(issuePath, guestSession))
                .doesNotContain("Mark as Closed")
                .doesNotContain("data-bs-target=\"#attachFileModal\"");
        assertThat(fetchPage("/users", guestSession)).doesNotContain(editLink);
    }

    // The edit page loads the user with getReferenceById (a lazy proxy);
    // the page must render another member's details, not the viewer's.
    @Test
    void projectManagerCanOpenAnotherMembersEditPage() throws Exception {
        Fixture fixture = createFixture();
        User developer = createUser(fixture.organization, "Developer");
        MockHttpSession session = login(fixture.projectManager.getEmail());

        String editPage = mockMvc.perform(get("/users/edit/" + developer.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("users/edit-user"))
                .andReturn().getResponse().getContentAsString();
        assertThat(editPage).contains(developer.getLastName());
    }

    @Test
    void editPageOfAnotherOrganizationsUserIsNotShown() throws Exception {
        Fixture fixture = createFixture();
        Fixture otherOrganization = createFixture();
        MockHttpSession session = login(fixture.projectManager.getEmail());

        MvcResult result = mockMvc.perform(get("/users/edit/" + otherOrganization.projectManager.getId()).session(session))
                .andReturn();

        String viewName = result.getModelAndView() == null ? null : result.getModelAndView().getViewName();
        assertThat(viewName).isNotEqualTo("users/edit-user");
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(otherOrganization.projectManager.getEmail());
    }

    @Test
    void guestCanViewAnIssueInTheirOrganization() throws Exception {
        Fixture fixture = createFixture();
        User guest = createUser(fixture.organization, "Guest");
        MockHttpSession session = login(guest.getEmail());

        mockMvc.perform(get("/issues/" + fixture.project.getIdentifier() + "/view/" + fixture.issue.getIssueKey()).session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("issues/issue-details"));
    }

    private String fetchPage(String path, MockHttpSession session) throws Exception {
        return mockMvc.perform(get(path).session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    // The create form always sends a "files" part, empty when nothing is attached.
    private static MockMultipartFile emptyFilesPart() {
        return new MockMultipartFile("files", "", "application/octet-stream", new byte[0]);
    }
}
