package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.Issue;
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
 * Mark as Resolved and Mark as Closed change only the status. They used to post the whole edit form, which has no
 * estimate or due date on the issue page, so both were cleared.
 */
class StatusChangeTest extends WebIntegrationTestBase {

    @Test
    void resolvingAnIssueKeepsEveryOtherField() throws Exception {
        Fixture fixture = createFixture();
        MockHttpSession session = login(fixture.projectManager.getEmail());

        mockMvc.perform(post(issuePath(fixture) + "/status").session(session).with(csrf()).param("status", "Resolved"))
                .andExpect(redirectedUrl(issuePath(fixture)));

        Issue reloaded = issueRepository.findIssueById(fixture.issue.getId());
        assertThat(reloaded.getStatus()).isEqualTo("Resolved");
        assertThat(reloaded.getTitle()).isEqualTo(fixture.issue.getTitle());
        assertThat(reloaded.getEstimatedHours()).isEqualTo(fixture.issue.getEstimatedHours());
        assertThat(reloaded.getDueDate()).isEqualTo(fixture.issue.getDueDate());
        assertThat(reloaded.getAssignedTo().getId()).isEqualTo(fixture.projectManager.getId());
        assertThat(reloaded.getUpdatedBy().getId()).isEqualTo(fixture.projectManager.getId());
    }

    @Test
    void developersCanResolveButNotClose() throws Exception {
        Fixture fixture = createFixture();
        User developer = createUser(fixture.organization, "Developer");
        fixture.project.addUser(developer);
        projectRepository.save(fixture.project);
        MockHttpSession session = login(developer.getEmail());

        mockMvc.perform(post(issuePath(fixture) + "/status").session(session).with(csrf()).param("status", "Closed"))
                .andExpect(status().isForbidden());
        assertThat(issueRepository.findIssueById(fixture.issue.getId()).getStatus()).isEqualTo("Open");

        mockMvc.perform(post(issuePath(fixture) + "/status").session(session).with(csrf()).param("status", "Resolved"))
                .andExpect(redirectedUrl(issuePath(fixture)));
        assertThat(issueRepository.findIssueById(fixture.issue.getId()).getStatus()).isEqualTo("Resolved");
    }

    @Test
    void otherStatusesAreRejected() throws Exception {
        Fixture fixture = createFixture();
        MockHttpSession session = login(fixture.projectManager.getEmail());

        mockMvc.perform(post(issuePath(fixture) + "/status").session(session).with(csrf()).param("status", "Deleted"))
                .andExpect(status().isBadRequest());

        assertThat(issueRepository.findIssueById(fixture.issue.getId()).getStatus()).isEqualTo("Open");
    }

    @Test
    void theIssuePagePostsTheStatusForms() throws Exception {
        Fixture fixture = createFixture();
        MockHttpSession session = login(fixture.projectManager.getEmail());

        String page = mockMvc.perform(get(issuePath(fixture)).session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(page)
                .contains("action=\"" + issuePath(fixture) + "/status\"")
                .contains("form=\"markResolved\"")
                .contains("form=\"markClosed\"")
                .doesNotContain("id=\"updateIssue\"");
    }

    private static String issuePath(Fixture fixture) {
        return "/issues/" + fixture.project.getIdentifier() + "/view/" + fixture.issue.getIssueKey();
    }
}
