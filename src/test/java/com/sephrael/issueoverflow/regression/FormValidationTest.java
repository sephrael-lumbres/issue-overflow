package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/**
 * Forms are checked in their handlers. A missing or too long value goes back to the form with a message instead of
 * being stored (a blank name, a null column) or failing (a 500 for a password over BCrypt's 72 bytes).
 */
class FormValidationTest extends WebIntegrationTestBase {

    // 40 characters, 80 bytes in UTF-8
    private static final String LONG_PASSWORD = "é".repeat(40);
    private static final Pattern RESET_TOKEN = Pattern.compile("change-password\\?token=([A-Za-z0-9_-]{43})");
    private static final String REGISTERED = "Your account is ready. Please log in.";
    private static final String ALREADY_REGISTERED = "An account with this email address already exists. Log in, or reset your password if you've forgotten it.";

    @Test
    void registrationWithAMissingNameIsRejected() throws Exception {
        String email = "new-" + uniqueSuffix().toLowerCase() + "@example.test";

        register("", "Example", email, "a-new-password")
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attributeExists("registrationError"));

        assertThat(userRepository.findByEmail(email)).isNull();
    }

    @Test
    void registrationWithAPasswordOver72BytesIsRejected() throws Exception {
        String email = "new-" + uniqueSuffix().toLowerCase() + "@example.test";

        register("Ada", "Example", email, LONG_PASSWORD)
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attributeExists("registrationError"));

        assertThat(userRepository.findByEmail(email)).isNull();
    }

    // A new address gets an account and a message on the login page;
    // an existing address is refused with a message and the existing account is untouched.
    @Test
    void registeringAnExistingAddressIsRefused() throws Exception {
        User existingUser = createFixture().projectManager;
        String newEmail = "new-" + uniqueSuffix().toLowerCase() + "@example.test";

        register("Ada", "Example", newEmail, "a-new-password")
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("registered", REGISTERED));

        register("Mallory", "Example", existingUser.getEmail(), "another-password")
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attribute("registrationError", ALREADY_REGISTERED));

        verify(mailSender, never()).send(any(MimeMessage.class));
        User reloadedUser = userRepository.findUserById(existingUser.getId());
        assertThat(reloadedUser.getFirstName()).isEqualTo(existingUser.getFirstName());
        assertThat(reloadedUser.getPassword()).isEqualTo(existingUser.getPassword());
    }

    @Test
    void aProfileWithABlankNameIsRejected() throws Exception {
        User user = createFixture().projectManager;
        MockHttpSession session = login(user.getEmail());

        mockMvc.perform(post("/account/profile/" + user.getId() + "/save").session(session).with(csrf())
                        .param("firstName", " ")
                        .param("lastName", user.getLastName())
                        .param("email", user.getEmail()))
                .andExpect(redirectedUrl("/account/profile/" + user.getId()))
                .andExpect(flash().attributeExists("profileChangeError"));

        assertThat(userRepository.findUserById(user.getId()).getFirstName()).isEqualTo(user.getFirstName());
    }

    @Test
    void aNewPasswordOver72BytesIsRejectedOnTheSecurityPage() throws Exception {
        User user = createFixture().projectManager;
        MockHttpSession session = login(user.getEmail());

        mockMvc.perform(post("/account/security/" + user.getId() + "/save").session(session).with(csrf())
                        .param("currentPassword", PASSWORD)
                        .param("newPassword", LONG_PASSWORD)
                        .param("confirmPassword", LONG_PASSWORD))
                .andExpect(redirectedUrl("/account/security/" + user.getId()))
                .andExpect(flash().attributeExists("error"));

        assertThat(userRepository.findUserById(user.getId()).getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    void aResetToAPasswordOver72BytesIsRejected() throws Exception {
        User user = createFixture().projectManager;
        mockMvc.perform(post("/reset-password").with(csrf()).param("email", user.getEmail()));
        Matcher token = RESET_TOKEN.matcher(sentEmailBody());
        assertThat(token.find()).as("reset link in the email").isTrue();

        mockMvc.perform(post("/change-password").with(csrf())
                        .param("token", token.group(1))
                        .param("password", LONG_PASSWORD)
                        .param("confirmPassword", LONG_PASSWORD))
                .andExpect(redirectedUrl("/change-password?token=" + token.group(1)))
                .andExpect(flash().attributeExists("passwordError"));

        assertThat(userRepository.findUserById(user.getId()).getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    void anOrganizationWithoutANameIsRejected() throws Exception {
        User user = createUser(null, "Guest");
        MockHttpSession session = login(user.getEmail());

        mockMvc.perform(post("/organization/save").session(session).with(csrf()).param("name", ""))
                .andExpect(redirectedUrl("/organization/new"))
                .andExpect(flash().attributeExists("organizationError"));

        assertThat(userRepository.findUserById(user.getId()).getOrganization()).isNull();
    }

    @Test
    void aProjectWithoutANameIsRejected() throws Exception {
        Fixture fixture = createFixture();
        MockHttpSession session = login(fixture.projectManager.getEmail());
        String identifier = "V" + uniqueSuffix();

        mockMvc.perform(post("/projects/new").session(session).with(csrf())
                        .param("name", "")
                        .param("identifier", identifier)
                        .param("color", "primary"))
                .andExpect(redirectedUrl("/projects/all"))
                .andExpect(flash().attributeExists("projectFormError"));

        assertThat(projectRepository.findByIdentifierAndOrganization(identifier, fixture.organization)).isNull();
    }

    @Test
    void anIssueWithoutATitleIsRejected() throws Exception {
        Fixture fixture = createFixture();
        MockHttpSession session = login(fixture.projectManager.getEmail());

        mockMvc.perform(multipart("/issues/new").file(new MockMultipartFile("files", "", "application/octet-stream", new byte[0]))
                        .session(session).with(csrf())
                        .param("title", " ")
                        .param("type", "Bug")
                        .param("priority", "High")
                        .param("status", "Open")
                        .param("project", fixture.project.getId().toString())
                        .param("assignedTo", " "))
                .andExpect(redirectedUrl("/issues/" + fixture.project.getIdentifier() + "/new"))
                .andExpect(flash().attributeExists("issueFormError"));

        assertThat(issueRepository.findByProject(fixture.project)).hasSize(1);
    }

    @Test
    void anEmptyCommentIsRejected() throws Exception {
        Fixture fixture = createFixture();
        MockHttpSession session = login(fixture.projectManager.getEmail());
        String issuePath = "/issues/" + fixture.project.getIdentifier() + "/view/" + fixture.issue.getIssueKey();

        mockMvc.perform(post(issuePath + "/comment/new").session(session).with(csrf()).param("message", ""))
                .andExpect(redirectedUrl(issuePath))
                .andExpect(flash().attributeExists("commentError"));

        assertThat(commentRepository.findByIssue(fixture.issue)).isEmpty();
    }

    private ResultActions register(String firstName, String lastName, String email, String password) throws Exception {
        return mockMvc.perform(post("/process_register").with(csrf())
                .param("firstName", firstName)
                .param("lastName", lastName)
                .param("email", email)
                .param("password", password));
    }
}
