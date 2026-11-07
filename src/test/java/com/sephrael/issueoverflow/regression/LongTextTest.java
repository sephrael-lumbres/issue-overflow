package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.AWSFile;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comments and file names were limited to 255 characters by the schema, so a longer one failed to save (a 500).
 */
class LongTextTest extends WebIntegrationTestBase {

    @Test
    void aCommentLongerThan255CharactersIsSaved() throws Exception {
        Fixture fixture = createFixture();
        MockHttpSession session = login(fixture.projectManager.getEmail());
        String message = "Steps to reproduce " + uniqueSuffix() + ": " + "x".repeat(300);

        mockMvc.perform(post("/issues/" + fixture.project.getIdentifier() + "/view/" + fixture.issue.getIssueKey() + "/comment/new")
                        .session(session).with(csrf())
                        .param("message", message))
                .andExpect(status().is3xxRedirection());

        assertThat(commentRepository.findCommentByMessage(message)).isNotNull();
    }

    @Test
    void anAttachmentWithALongNameIsStoredUnderAShorterName() {
        Fixture fixture = createFixture();
        String longName = "n".repeat(300) + ".txt";

        AWSFile awsFile = uploadAttachment(fixture, longName, "text/plain", "notes".getBytes(StandardCharsets.UTF_8));

        assertThat(awsFile.getFileName()).hasSize(255).startsWith("nnn").endsWith(".txt");
    }
}
