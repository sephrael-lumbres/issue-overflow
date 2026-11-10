package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.Issue;
import com.sephrael.issueoverflow.entity.Organization;
import com.sephrael.issueoverflow.entity.Project;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Notifications go to every project member who verified their address and asked for that event and that issue.
 * The old code built an array with gaps, sent nothing unless the first member was a recipient,
 * and read the "issues created by you" box before (and instead of) the "issues assigned to you" box.
 */
class NotificationRecipientsTest extends WebIntegrationTestBase {

    @Test
    void membersAreNotifiedEvenWhenTheFirstMemberIsNot() throws Exception {
        Organization organization = createOrganization();
        User projectManager = createUser(organization, "Project Manager");
        Project project = createProject(organization, projectManager);
        User developer = optIn(createUser(organization, "Developer"), true, false, true, false, false);
        project.addUser(developer);
        projectRepository.save(project);
        MockHttpSession session = login(projectManager.getEmail());

        mockMvc.perform(multipart("/issues/new").file(new MockMultipartFile("files", "", "application/octet-stream", new byte[0]))
                        .session(session).with(csrf())
                        .param("title", "Notify the developer " + uniqueSuffix())
                        .param("project", project.getId().toString())
                        .param("assignedTo", " "))
                .andExpect(status().is3xxRedirection());

        assertThat(sentTo()).containsExactly(developer.getEmail());
    }

    @Test
    void theAssignedBoxCountsWhenTheAuthoredBoxIsAlsoTicked() throws Exception {
        Organization organization = createOrganization();
        User projectManager = optIn(createUser(organization, "Project Manager"), false, true, true, false, false);
        Project project = createProject(organization, projectManager);
        User developer = optIn(createUser(organization, "Developer"), false, true, false, true, true);
        project.addUser(developer);
        projectRepository.save(project);
        Issue issue = createIssue(project, projectManager, developer, "Bug", "Open", "High");
        MockHttpSession session = login(projectManager.getEmail());

        mockMvc.perform(post("/issues/update").session(session).with(csrf())
                        .param("id", issue.getId().toString())
                        .param("title", "Updated for the assignee")
                        .param("project", project.getId().toString())
                        .param("assignedTo", developer.getId().toString()))
                .andExpect(status().is3xxRedirection());

        assertThat(sentTo()).containsExactlyInAnyOrder(projectManager.getEmail(), developer.getEmail());
    }

    // Verified, with the given "when" (created, updated) and "which issues" (all, assigned, authored) boxes ticked.
    private User optIn(User user, boolean created, boolean updated, boolean allIssues, boolean assigned, boolean authored) {
        user.setEmailVerified(true);
        user.setIsCreatedEnabled(created);
        user.setIsUpdatedEnabled(updated);
        user.setIsAllIssuesEnabled(allIssues);
        user.setIsAssignedIssuesEnabled(assigned);
        user.setIsAuthoredIssuesEnabled(authored);
        return userRepository.save(user);
    }

    // The To addresses of the one email sent in this test.
    private List<String> sentTo() throws Exception {
        ArgumentCaptor<MimeMessage> message = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(message.capture());
        Address[] recipients = message.getValue().getRecipients(Message.RecipientType.TO);
        return Arrays.stream(recipients).map(Address::toString).toList();
    }
}
