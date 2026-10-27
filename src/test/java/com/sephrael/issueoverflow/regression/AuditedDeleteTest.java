package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

// Envers writes audit rows when a transaction commits, so the rolled-back repository tests no longer exercise them.
// This test commits a delete of an audited entity (and, by cascade, its issue)
// and checks the delete revisions (revtype 2) were written.
class AuditedDeleteTest extends WebIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deletingAProjectRecordsDeleteRevisionsForItAndItsIssues() {
        Fixture fixture = createFixture();

        projectRepository.deleteById(fixture.project.getId());

        assertThat(projectRepository.findById(fixture.project.getId())).isEmpty();
        assertThat(issueRepository.findById(fixture.issue.getId())).isEmpty();
        assertThat(lastRevisionType("project_aud", fixture.project.getId())).isEqualTo(2);
        assertThat(lastRevisionType("issue_aud", fixture.issue.getId())).isEqualTo(2);
    }

    private Integer lastRevisionType(String auditTable, Long id) {
        return jdbcTemplate.queryForObject(
                "select revtype from " + auditTable + " where id = ? order by rev desc limit 1",
                Integer.class, id);
    }
}
