package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The JSON API's error answers, through the test-only ApiTestProbeController: problem-details bodies with the right
 * status, the same 404 for another organization's record as for a missing one, and HTML error pages unchanged.
 */
class ApiErrorsTest extends WebIntegrationTestBase {

    @Test
    void aMemberGetsTheIssueAsJson() throws Exception {
        Fixture fixture = createFixture();
        MockHttpSession session = login(fixture.projectManager.getEmail());

        mockMvc.perform(get("/api/probe/issues/" + fixture.issue.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(fixture.issue.getId()))
                .andExpect(jsonPath("$.title").value(fixture.issue.getTitle()));
    }

    // Another organization's issue, an issue in a project the caller isn't a member of, and an id that doesn't exist
    // all get the same 404 body (only "instance", the request path, differs).
    @Test
    void anotherOrganizationsIssueLooksMissing() throws Exception {
        Fixture victim = createFixture();
        Fixture caller = createFixture();
        User nonMember = createUser(victim.organization, "Developer");
        MockHttpSession callerSession = login(caller.projectManager.getEmail());
        MockHttpSession nonMemberSession = login(nonMember.getEmail());

        expectNotFound(callerSession, "/api/probe/issues/" + victim.issue.getId());
        expectNotFound(callerSession, "/api/probe/issues/" + Long.MAX_VALUE);
        expectNotFound(nonMemberSession, "/api/probe/issues/" + victim.issue.getId());
    }

    @Test
    void anInvalidBodyListsTheFieldsAndConstraints() throws Exception {
        MockHttpSession session = login(createFixture().projectManager.getEmail());

        mockMvc.perform(post("/api/probe/echo").session(session).with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("The request has invalid fields."))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("text"))
                .andExpect(jsonPath("$.errors[0].constraint").value("NotBlank"))
                .andExpect(jsonPath("$.errors[0].message").doesNotExist());
    }

    @Test
    void forbiddenAndUnexpectedErrorsAreProblems() throws Exception {
        MockHttpSession session = login(createFixture().projectManager.getEmail());

        mockMvc.perform(get("/api/probe/forbidden").session(session))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Forbidden"));

        String body = mockMvc.perform(get("/api/probe/failure").session(session))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Something went wrong."))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("internal detail");
    }

    // The advice covers only the API package: a page's 404 is still left to the HTML error page, not answered as JSON.
    @Test
    void pagesKeepTheirHtmlErrorPages() throws Exception {
        MockHttpSession session = login(createFixture().projectManager.getEmail());

        String body = mockMvc.perform(get("/projects/NO-SUCH-PROJECT").session(session))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).isEmpty();
    }

    private void expectNotFound(MockHttpSession session, String path) throws Exception {
        mockMvc.perform(get(path).session(session))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").doesNotExist())
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").doesNotExist())
                .andExpect(jsonPath("$.instance").value(path));
    }
}
