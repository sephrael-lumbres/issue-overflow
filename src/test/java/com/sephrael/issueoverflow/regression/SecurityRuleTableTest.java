package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import jakarta.servlet.http.HttpServletRequest;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * One row per request rule in WebSecurityConfig: which callers the rule lets through. The rows ask the chain's
 * own AuthorizationManager, so no controller runs and nothing is written. An anonymous caller that is denied
 * is sent to the login page by the filter chain; a signed-in caller that is denied gets 403.
 * <p>
 * The trailing-slash rows record what the rules do with a path they don't name: it falls through to
 * "any authenticated user". The MVC test below shows that no controller answers those paths.
 */
class SecurityRuleTableTest extends WebIntegrationTestBase {

    private static final boolean Y = true;
    private static final boolean N = false;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    //          row(<path>,                                       anonymous, guest, dev, admin, pm)
    static Stream<Arguments> rules() {
        return Stream.of(
                // public pages and assets
                row("/register",                                          Y, Y, Y, Y, Y),
                row("/login",                                             Y, Y, Y, Y, Y),
                row("/process_register",                                  Y, Y, Y, Y, Y),
                row("/privacy",                                           Y, Y, Y, Y, Y),
                row("/cookie-policy",                                     Y, Y, Y, Y, Y),
                row("/getting-started",                                   Y, Y, Y, Y, Y),
                row("/getting-started/introduction",                      Y, Y, Y, Y, Y),
                row("/knowledge-base/projects/creating-a-project",        Y, Y, Y, Y, Y),
                row("/contact-us",                                        Y, Y, Y, Y, Y),
                row("/reset-password",                                    Y, Y, Y, Y, Y),
                row("/change-password",                                   Y, Y, Y, Y, Y),
                row("/verify-email",                                      Y, Y, Y, Y, Y),
                row("/error",                                             Y, Y, Y, Y, Y),
                row("/js/scripts.js",                                     Y, Y, Y, Y, Y),
                row("/assets/img/swirl-logo.png",                         Y, Y, Y, Y, Y),
                row("/css/styles.css",                                    Y, Y, Y, Y, Y),

                // Project Manager only
                row("/users/edit/1",                                      N, N, N, N, Y),
                row("/users/update/1",                                    N, N, N, N, Y),
                row("/users/remove/1",                                    N, N, N, N, Y),
                row("/users/remove/1/1",                                  N, N, N, N, Y),
                row("/organization/update",                               N, N, N, N, Y),
                row("/projects/new",                                      N, N, N, N, Y),
                row("/projects/save",                                     N, N, N, N, Y),
                row("/projects/delete/1",                                 N, N, N, N, Y),

                // issue and attachment writes: Project Manager, Admin, Developer
                row("/issues/new",                                        N, N, Y, Y, Y),
                row("/issues/update",                                     N, N, Y, Y, Y),
                row("/issues/PRJ/new",                                    N, N, Y, Y, Y),
                row("/issues/PRJ/edit/PRJ-1",                             N, N, Y, Y, Y),
                row("/issues/PRJ/delete/PRJ-1/false",                     N, N, Y, Y, Y),
                row("/storage/PRJ/PRJ-1/delete/some-key",                 N, N, Y, Y, Y),

                // issue page and comments: every role
                row("/issues/PRJ/view/PRJ-1",                             N, Y, Y, Y, Y),
                row("/issues/PRJ/view/PRJ-1/comment/new",                 N, Y, Y, Y, Y),
                row("/issues/PRJ/view/PRJ-1/comment/save",                N, Y, Y, Y, Y),
                row("/issues/PRJ/view/PRJ-1/comment/1/delete",            N, Y, Y, Y, Y),

                // everything else: any signed-in user
                row("/dashboard",                                         N, Y, Y, Y, Y),
                row("/issues/all",                                        N, Y, Y, Y, Y),
                row("/storage/files/some-key",                            N, Y, Y, Y, Y),
                row("/storage/upload/1/true",                             N, Y, Y, Y, Y),
                row("/organization/joining",                              N, Y, Y, Y, Y),
                row("/account/profile/1",                                 N, Y, Y, Y, Y),
                row("/resend-verification-email",                         N, Y, Y, Y, Y),

                // trailing slashes: unnamed paths fall through to "any signed-in user", except under a "/**" rule
                row("/organization/update/",                              N, Y, Y, Y, Y),
                row("/projects/save/",                                    N, Y, Y, Y, Y),
                row("/issues/PRJ/edit/PRJ-1/",                            N, Y, Y, Y, Y),
                row("/users/remove/1/",                                   N, N, N, N, Y)
        );
    }

    private static Arguments row(String path, boolean anonymous, boolean guest, boolean developer, boolean admin, boolean projectManager) {
        return Arguments.of(path, anonymous, guest, developer, admin, projectManager);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rules")
    void ruleAllowsExactlyTheseCallers(String path, boolean anonymous, boolean guest, boolean developer, boolean admin, boolean projectManager) {
        AuthorizationManager<HttpServletRequest> manager = authorizationManager();
        Map<String, Boolean> expected = new LinkedHashMap<>();
        expected.put("anonymous", anonymous);
        expected.put("Guest", guest);
        expected.put("Developer", developer);
        expected.put("Admin", admin);
        expected.put("Project Manager", projectManager);

        SoftAssertions softly = new SoftAssertions();
        for (Map.Entry<String, Boolean> caller : expected.entrySet()) {
            softly.assertThat(isGranted(manager, path, caller.getKey()))
                    .as("%s on %s", caller.getKey(), path)
                    .isEqualTo(caller.getValue());
        }
        softly.assertAll();
    }

    // The rules above let any signed-in user reach these paths, and no controller maps them. GET ends in 404.
    // POST ends in 404 or 405 (the static resource handler matches any path but takes only GET and HEAD). Nothing changes.
    @Test
    void trailingSlashVariantsAreNotRouted() throws Exception {
        Fixture fixture = createFixture();
        User guest = createUser(fixture.organization, "Guest");
        MockHttpSession session = login(guest.getEmail());

        assertNotRouted(mockMvc.perform(post("/organization/update/").session(session).with(csrf())
                .param("id", fixture.organization.getId().toString())
                .param("organizationName", "Renamed through a trailing slash")).andReturn());
        assertNotRouted(mockMvc.perform(post("/projects/save/").session(session).with(csrf())
                .param("id", fixture.project.getId().toString())
                .param("name", "Renamed through a trailing slash")
                .param("identifier", fixture.project.getIdentifier())
                .param("color", "red")).andReturn());
        assertNotRouted(mockMvc.perform(get("/issues/" + fixture.project.getIdentifier()
                + "/edit/" + fixture.issue.getIssueKey() + "/").session(session)).andReturn());

        assertThat(organizationRepository.findOrganizationById(fixture.organization.getId()).getName())
                .isEqualTo(fixture.organization.getName());
        assertThat(projectRepository.findProjectById(fixture.project.getId()).getName())
                .isEqualTo(fixture.project.getName());
    }

    @Test
    void anonymousVisitorCanOpenTheVerifyEmailLink() throws Exception {
        mockMvc.perform(get("/verify-email").param("emailVerificationToken", "not-a-real-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("account-settings/verified-email"))
                .andExpect(model().attribute("userIsNull", true));
    }

    // Security let the request through (not 403); MVC had no handler for it.
    private static void assertNotRouted(MvcResult result) {
        assertThat(result.getResponse().getStatus())
                .as("status of %s %s", result.getRequest().getMethod(), result.getRequest().getRequestURI())
                .isIn(404, 405);
    }

    private AuthorizationManager<HttpServletRequest> authorizationManager() {
        return securityFilterChain.getFilters().stream()
                .filter(AuthorizationFilter.class::isInstance)
                .map(AuthorizationFilter.class::cast)
                .findFirst()
                .orElseThrow()
                .getAuthorizationManager();
    }

    private static boolean isGranted(AuthorizationManager<HttpServletRequest> manager, String path, String caller) {
        Authentication authentication = "anonymous".equals(caller)
                ? new AnonymousAuthenticationToken("rule-table", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"))
                : UsernamePasswordAuthenticationToken.authenticated("rule-table-user", null,
                AuthorityUtils.createAuthorityList(caller));
        AuthorizationResult result = manager.authorize(() -> authentication, new MockHttpServletRequest("GET", path));
        return result != null && result.isGranted();
    }
}
