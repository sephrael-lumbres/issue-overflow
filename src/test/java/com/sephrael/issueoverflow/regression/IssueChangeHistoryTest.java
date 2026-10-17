package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.Issue;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.service.IssueService;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.history.RevisionMetadata;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class IssueChangeHistoryTest extends WebIntegrationTestBase {

    @Autowired
    private IssueService issueService;

    @Test
    void historyListsEachChangedFieldWithOldAndNewValues() {
        Fixture fixture = createFixture();                // revision 1: insert (Open, High)
        User editor = createUser(fixture.organization, "Developer");
        Long issueId = fixture.issue.getId();
        String originalTitle = fixture.issue.getTitle();

        editIssueTitle(issueId, "Edited title", editor);  // revision 2: title, updatedBy
        inTransaction(() -> {                             // revision 3: priority, status
            Issue issue = issueRepository.findById(issueId).orElseThrow();
            issue.setPriority("Low");
            issue.setStatus("In-Progress");
        });

        List<Issue> history = issueService.getIssueChangeHistoryList(issueId);

        assertThat(history)
                .extracting(Issue::getProperty, Issue::getOldValue, Issue::getNewValue)
                .containsExactly(
                        tuple("Title", originalTitle, "Edited title"),
                        tuple("Priority", "High", "Low"),
                        tuple("Status", "Open", "In-Progress"));
        assertThat(history).allSatisfy(entry ->
                assertThat(entry.getUpdatedBy().getId()).isEqualTo(editor.getId()));
        // Each entry carries the metadata of the older revision in its pair.
        assertThat(history.get(0).getChangeVersion().getRevisionType()).isEqualTo(RevisionMetadata.RevisionType.INSERT);
        assertThat(history.get(1).getChangeVersion().getRevisionType()).isEqualTo(RevisionMetadata.RevisionType.UPDATE);
        assertThat(history.get(2).getChangeVersion().getRevisionType()).isEqualTo(RevisionMetadata.RevisionType.UPDATE);
        assertThat(history).allSatisfy(entry ->
                assertThat(entry.getChangeVersion().getRequiredRevisionInstant()).isNotNull());
    }
}
