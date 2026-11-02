package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Links in emails start with app.base-url (https://issue-overflow.test in the test profile), whatever host the
 * request claims; the link tests forge the Host header as evil.example. User text in the HTML body is escaped.
 */
class EmailLinksTest extends WebIntegrationTestBase {

    private static final String BASE_URL = "https://issue-overflow.test";

    private static RequestPostProcessor forgedHost() {
        return request -> {
            request.setServerName("evil.example");
            request.addHeader("Host", "evil.example");
            return request;
        };
    }

    @Test
    void passwordResetLinkUsesTheConfiguredBaseUrl() throws Exception {
        User user = createFixture().projectManager;

        mockMvc.perform(post("/reset-password").with(csrf()).with(forgedHost()).param("email", user.getEmail()));

        assertThat(sentEmailBody())
                .contains(BASE_URL + "/change-password?token=")
                .doesNotContain("evil.example");
    }

    @Test
    void verificationLinkUsesTheConfiguredBaseUrl() throws Exception {
        User user = createFixture().projectManager;
        MockHttpSession session = login(user.getEmail());

        mockMvc.perform(post("/account/notifications/" + user.getId() + "/save").session(session).with(csrf())
                .with(forgedHost())
                .param("created", "on"));

        assertThat(sentEmailBody())
                .contains(BASE_URL + "/verify-email?emailVerificationToken=")
                .doesNotContain("evil.example");
    }

    @Test
    void notificationLinkUsesTheConfiguredBaseUrl() throws Exception {
        Fixture fixture = createFixture();
        User manager = userRepository.findUserById(fixture.projectManager.getId());
        manager.setEmailVerified(true);
        manager.setIsUpdatedEnabled(true);
        manager.setIsAllIssuesEnabled(true);
        userRepository.save(manager);
        MockHttpSession session = login(manager.getEmail());

        mockMvc.perform(post("/issues/update").session(session).with(csrf()).with(forgedHost())
                .param("id", fixture.issue.getId().toString())
                .param("title", "Updated title")
                .param("project", fixture.project.getId().toString())
                .param("assignedTo", " "));

        assertThat(sentEmailBody())
                .contains(BASE_URL + "/issues/" + fixture.project.getIdentifier() + "/view/" + fixture.issue.getIssueKey())
                .doesNotContain("evil.example");
    }

    // Names, titles and keys are user input; the email body is HTML.
    @Test
    void notificationEscapesUserText() throws Exception {
        Fixture fixture = createFixture();
        User manager = userRepository.findUserById(fixture.projectManager.getId());
        manager.setLastName("<img src=x onerror=alert(1)>");
        manager.setEmailVerified(true);
        manager.setIsUpdatedEnabled(true);
        manager.setIsAllIssuesEnabled(true);
        userRepository.save(manager);
        MockHttpSession session = login(manager.getEmail());

        mockMvc.perform(post("/issues/update").session(session).with(csrf())
                .param("id", fixture.issue.getId().toString())
                .param("title", "Updated title")
                .param("project", fixture.project.getId().toString())
                .param("assignedTo", " "));

        assertThat(sentEmailBody())
                .contains("&lt;img src=x onerror=alert(1)&gt;")
                .doesNotContain("<img");
    }

    // The contact form is anonymous; its message goes to the app's own support address.
    @Test
    void contactFormEscapesUserText() throws Exception {
        mockMvc.perform(post("/contact-us/send-message").with(csrf())
                .param("name", "<b>Mallory</b>")
                .param("email", "mallory@example.test")
                .param("message", "<img src=x onerror=alert(1)>\nsecond line"));

        assertThat(sentEmailBody())
                .contains("&lt;img src=x onerror=alert(1)&gt;<br>second line")
                .contains("From: &lt;b&gt;Mallory&lt;/b&gt;")
                .doesNotContain("<img")
                .doesNotContain("<b>");
    }
}
