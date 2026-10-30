package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.AWSFile;
import com.sephrael.issueoverflow.entity.Comment;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.mock.web.MockHttpSession;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The five delete routes now take POST only, so that Spring Security's CSRF check covers them;
 * a link or an image tag can no longer delete anything. The pages submit small forms whose
 * buttons carry the confirmation text in a data attribute instead of an inline onclick handler.
 */
class DeleteRoutesTest extends WebIntegrationTestBase {

    @Test
    void getRequestsNoLongerDeleteAnything() throws Exception {
        Fixture fixture = createFixture();
        User developer = createUser(fixture.organization, "Developer");
        Comment comment = createComment(fixture.issue, fixture.projectManager, "Keep me");
        AWSFile attachment = uploadAttachment(fixture, "notes.txt", "text/plain", "notes".getBytes(StandardCharsets.UTF_8));
        MockHttpSession session = login(fixture.projectManager.getEmail());
        String projectIdentifier = fixture.project.getIdentifier();
        String issueKey = fixture.issue.getIssueKey();

        for (String path : List.of(
                "/users/remove/" + developer.getId(),
                "/issues/" + projectIdentifier + "/view/" + issueKey + "/comment/" + comment.getId() + "/delete",
                "/issues/" + projectIdentifier + "/delete/" + issueKey + "/false",
                "/storage/" + projectIdentifier + "/" + issueKey + "/delete/" + attachment.getFileKey(),
                "/projects/delete/" + fixture.project.getId())) {
            mockMvc.perform(get(path).session(session))
                    .andExpect(status().isMethodNotAllowed());
        }

        assertThat(userRepository.findUserById(developer.getId()).getOrganization()).isNotNull();
        assertThat(commentRepository.findCommentById(comment.getId())).isNotNull();
        assertThat(issueRepository.findIssueById(fixture.issue.getId())).isNotNull();
        assertThat(awsFileRepository.findByFileKey(attachment.getFileKey())).isNotNull();
        assertThat(projectRepository.findProjectById(fixture.project.getId())).isNotNull();
    }

    @Test
    void postDeletesAnIssue() throws Exception {
        Fixture fixture = createFixture();
        // a second issue keeps the project non-empty, so its key sequence (which fixtures don't create) is left alone
        createIssue(fixture.project, fixture.projectManager, null, "Task", "Open", "Low", 2);
        MockHttpSession session = login(fixture.projectManager.getEmail());

        mockMvc.perform(post("/issues/" + fixture.project.getIdentifier() + "/delete/" + fixture.issue.getIssueKey() + "/false")
                        .session(session).with(csrf()))
                .andExpect(redirectedUrl("/issues/" + fixture.project.getIdentifier()));

        assertThat(issueRepository.findIssueById(fixture.issue.getId())).isNull();
    }

    @Test
    void postDeletesAProject() throws Exception {
        Fixture fixture = createFixture();
        MockHttpSession session = login(fixture.projectManager.getEmail());

        mockMvc.perform(post("/projects/delete/" + fixture.project.getId()).session(session).with(csrf()))
                .andExpect(redirectedUrl("/projects/all"));

        assertThat(projectRepository.findProjectById(fixture.project.getId())).isNull();
    }

    @Test
    void postDeletesYourOwnComment() throws Exception {
        Fixture fixture = createFixture();
        Comment comment = createComment(fixture.issue, fixture.projectManager, "Delete me");
        MockHttpSession session = login(fixture.projectManager.getEmail());
        String issuePath = "/issues/" + fixture.project.getIdentifier() + "/view/" + fixture.issue.getIssueKey();

        mockMvc.perform(post(issuePath + "/comment/" + comment.getId() + "/delete").session(session).with(csrf()))
                .andExpect(redirectedUrl(issuePath));

        assertThat(commentRepository.findCommentById(comment.getId())).isNull();
    }

    @Test
    void postRemovesAMemberFromYourOrganization() throws Exception {
        Fixture fixture = createFixture();
        User developer = createUser(fixture.organization, "Developer");
        MockHttpSession session = login(fixture.projectManager.getEmail());

        mockMvc.perform(post("/users/remove/" + developer.getId()).session(session).with(csrf()))
                .andExpect(redirectedUrl("/users"));

        User reloadedUser = userRepository.findUserById(developer.getId());
        assertThat(reloadedUser.getOrganization()).isNull();
        assertThat(reloadedUser.getRole().getName()).isEqualTo("Guest");
        assertThat(organizationRepository.findOrganizationById(fixture.organization.getId())).isNotNull();
    }

    @Test
    void postDeletesYourOwnAttachment() throws Exception {
        Fixture fixture = createFixture();
        AWSFile attachment = uploadAttachment(fixture, "notes.txt", "text/plain", "notes".getBytes(StandardCharsets.UTF_8));
        MockHttpSession session = login(fixture.projectManager.getEmail());
        String projectIdentifier = fixture.project.getIdentifier();
        String issueKey = fixture.issue.getIssueKey();

        mockMvc.perform(post("/storage/" + projectIdentifier + "/" + issueKey + "/delete/" + attachment.getFileKey())
                        .session(session).with(csrf()))
                .andExpect(redirectedUrl("/issues/" + projectIdentifier + "/view/" + issueKey));

        assertThat(awsFileRepository.findByFileKey(attachment.getFileKey())).isNull();
    }

    // Names and file names used to sit inside an inline "return confirm('...')" handler, where a quote in the name ended the string.
    @Test
    void deleteButtonsCarryTheirConfirmationAsData() throws Exception {
        Fixture fixture = createFixture();
        User developer = createUser(fixture.organization, "Developer");
        developer.setLastName("O'Brien');alert(1);//");
        userRepository.save(developer);
        uploadAttachment(fixture, "it's');alert(2);//.txt", "text/plain", "notes".getBytes(StandardCharsets.UTF_8));
        MockHttpSession session = login(fixture.projectManager.getEmail());
        String projectIdentifier = fixture.project.getIdentifier();
        String issueKey = fixture.issue.getIssueKey();

        for (String path : List.of(
                "/dashboard",
                "/users",
                "/projects/all",
                "/issues/all",
                "/issues/" + projectIdentifier,
                "/issues/" + projectIdentifier + "/view/" + issueKey,
                "/issues/" + projectIdentifier + "/edit/" + issueKey)) {
            String page = mockMvc.perform(get(path).session(session))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(page).as(path).contains("data-confirm=").contains("/js/confirm-submit.js").doesNotContain("return confirm(");
        }

        Resource[] templates = new PathMatchingResourcePatternResolver().getResources("classpath*:templates/**/*.html");
        assertThat(templates).isNotEmpty();
        for (Resource template : templates) {
            String source = template.getContentAsString(StandardCharsets.UTF_8);
            assertThat(source).as(template.getFilename()).doesNotContain("return confirm(");
        }
    }
}
