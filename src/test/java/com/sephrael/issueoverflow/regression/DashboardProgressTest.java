package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.Organization;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The dashboard's progress tracker shows the share of each project's issues that are closed.
 * A project without issues divided 0 by 0 and showed NaN instead of a percentage.
 */
class DashboardProgressTest extends WebIntegrationTestBase {

    @Test
    void aProjectWithoutIssuesShowsNoProgress() throws Exception {
        Organization organization = createOrganization();
        User projectManager = createUser(organization, "Project Manager");
        createProject(organization, projectManager);

        String page = mockMvc.perform(get("/dashboard").session(login(projectManager.getEmail())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(page).contains("style=\"width: 0%\"").doesNotContain("NaN");
    }
}
