package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Requests to the JSON API that the security layer turns away now get a 401 or 403 problem-details body,
 * not the login page or the HTML 403 page, and are never saved as the page to return to after logging in.
 */
class ApiSecurityTest extends WebIntegrationTestBase {

    @Test
    void anonymousRequestsGetA401Problem() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(get("/api/probe/issues/" + fixture.issue.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.detail").value("Not logged in. Please log in to continue."));
    }

    // A changed password makes every other session stale. On a JSON request the session is logged out with a 401.
    @Test
    void aStaleSessionGetsA401ProblemAndIsLoggedOut() throws Exception {
        Fixture fixture = createFixture();
        MockHttpSession session = login(fixture.projectManager.getEmail());
        inTransaction(() -> {
            User user = userRepository.findUserById(fixture.projectManager.getId());
            user.setPassword(new BCryptPasswordEncoder().encode("a-changed-password"));
        });

        mockMvc.perform(get("/api/probe/issues/" + fixture.issue.getId()).session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Your session has expired. Please log in again to view this page."));

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void aPostWithoutACsrfTokenGetsA403Problem() throws Exception {
        MockHttpSession session = login(createFixture().projectManager.getEmail());

        mockMvc.perform(post("/api/probe/echo").session(session).contentType(MediaType.APPLICATION_JSON).content("{\"text\": \"hello\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Invalid or missing CSRF token. Request aborted."));

        mockMvc.perform(post("/api/probe/echo").session(session).with(csrf().asHeader()).contentType(MediaType.APPLICATION_JSON).content("{\"text\": \"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("hello"));
    }

    // A page asked for while logged out is where the login lands; a JSON request asked for while logged out is not.
    @Test
    void onlyPagesAreReturnedToAfterLogin() throws Exception {
        Fixture fixture = createFixture();

        MockHttpSession apiSession = new MockHttpSession();
        mockMvc.perform(get("/api/probe/issues/" + fixture.issue.getId()).session(apiSession))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/login").session(apiSession).with(csrf())
                        .param("email", fixture.projectManager.getEmail())
                        .param("password", PASSWORD))
                .andExpect(redirectedUrl("/dashboard"));

        MockHttpSession pageSession = new MockHttpSession();
        mockMvc.perform(get("/issues/all").session(pageSession))
                .andExpect(status().is3xxRedirection());
        String target = mockMvc.perform(post("/login").session(pageSession).with(csrf())
                        .param("email", fixture.projectManager.getEmail())
                        .param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        assertThat(target).endsWith("/issues/all?continue");

        // a request browser tools make on their own (Chrome DevTools) isn't a page to return to either
        MockHttpSession probeSession = new MockHttpSession();
        mockMvc.perform(get("/.well-known/appspecific/com.chrome.devtools.json").session(probeSession))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/login").session(probeSession).with(csrf())
                        .param("email", fixture.projectManager.getEmail())
                        .param("password", PASSWORD))
                .andExpect(redirectedUrl("/dashboard"));
    }

    // A page asked for without an HTML Accept header (curl, fetch, MockMvc's default) still redirects to the login page.
    @Test
    void pagesRedirectToLoginWhateverTheyAccept() throws Exception {
        for (String accept : new String[] {"*/*", "application/json"}) {
            mockMvc.perform(get("/dashboard").header("Accept", accept)).andExpect(redirectedUrl("/login"));
        }
    }
}
