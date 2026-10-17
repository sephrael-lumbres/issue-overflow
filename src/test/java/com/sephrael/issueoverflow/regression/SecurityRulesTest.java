package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityRulesTest extends WebIntegrationTestBase {

    @Test
    void anonymousUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void publicPagesAreReachableAnonymously() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
        mockMvc.perform(get("/reset-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"));
        // Passes security ("/change-password*") and reaches the controller, which
        // redirects to the relative "/login" (security would redirect to an absolute URL).
        mockMvc.perform(get("/change-password").param("token", "not-a-real-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void validCredentialsLogInWithTheUsersRole() throws Exception {
        Fixture fixture = createFixture();
        String email = fixture.projectManager.getEmail();

        MvcResult result = mockMvc.perform(formLogin("/login").userParameter("email").user(email).password(PASSWORD))
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(authenticated().withUsername(email))
                .andReturn();

        SecurityContext context = (SecurityContext) Objects.requireNonNull(result.getRequest().getSession(false))
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(context).isNotNull();
        // "contains", not "equals": Spring Security 7 adds a FACTOR_PASSWORD authority.
        List<String> authorities = context.getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        assertThat(authorities).contains("Project Manager");
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        Fixture fixture = createFixture();

        mockMvc.perform(formLogin("/login").userParameter("email").user(fixture.projectManager.getEmail()).password("wrong-password"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void guestCannotUpdateIssues() throws Exception {
        Fixture fixture = createFixture();
        User guest = createUser(fixture.organization, "Guest");
        MockHttpSession session = login(guest.getEmail());

        mockMvc.perform(post("/issues/update").session(session).with(csrf())
                        .param("id", fixture.issue.getId().toString())
                        .param("title", "Changed by a guest"))
                .andExpect(status().isForbidden());

        assertThat(issueRepository.findById(fixture.issue.getId()).orElseThrow().getTitle())
                .isEqualTo(fixture.issue.getTitle());
    }

    @Test
    void developerCannotUpdateOrganization() throws Exception {
        Fixture fixture = createFixture();
        User developer = createUser(fixture.organization, "Developer");
        MockHttpSession session = login(developer.getEmail());

        mockMvc.perform(post("/organization/update").session(session).with(csrf())
                        .param("id", fixture.organization.getId().toString())
                        .param("organizationName", "Renamed by a developer"))
                .andExpect(status().isForbidden());

        assertThat(organizationRepository.findOrganizationById(fixture.organization.getId()).getName())
                .isEqualTo(fixture.organization.getName());
    }

    @Test
    void projectManagerCanUpdateOrganizationWithCsrfToken() throws Exception {
        Fixture fixture = createFixture();
        MockHttpSession session = login(fixture.projectManager.getEmail());
        String newName = "Renamed " + uniqueSuffix();

        mockMvc.perform(post("/organization/update").session(session).with(csrf())
                        .param("id", fixture.organization.getId().toString())
                        .param("organizationName", newName))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/organization"));

        assertThat(organizationRepository.findOrganizationById(fixture.organization.getId()).getName())
                .isEqualTo(newName);
    }

    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        Fixture fixture = createFixture();
        MockHttpSession session = login(fixture.projectManager.getEmail());

        mockMvc.perform(post("/organization/update").session(session)
                        .param("id", fixture.organization.getId().toString())
                        .param("organizationName", "Renamed without a token"))
                .andExpect(status().isForbidden());

        assertThat(organizationRepository.findOrganizationById(fixture.organization.getId()).getName())
                .isEqualTo(fixture.organization.getName());
    }

    // Creating an organization makes the creator a Project Manager and logs them out so the new role takes effect at
    // the next login. Spring Security 6 stops treating a cleared SecurityContextHolder as a logout; this test guards it.
    @Test
    void creatingAnOrganizationLogsTheUserOut() throws Exception {
        User newcomer = createUser(null, "Guest");
        MockHttpSession session = login(newcomer.getEmail());

        mockMvc.perform(post("/organization/save").session(session).with(csrf())
                        .param("name", "Created " + uniqueSuffix()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        User reloaded = userRepository.findByEmail(newcomer.getEmail());
        assertThat(reloaded.getOrganization()).isNotNull();
        assertThat(reloaded.getRole().getName()).isEqualTo("Project Manager");
    }
}
