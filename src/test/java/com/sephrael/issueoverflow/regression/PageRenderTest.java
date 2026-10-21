package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
