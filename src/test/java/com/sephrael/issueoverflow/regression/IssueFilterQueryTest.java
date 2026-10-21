package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.Issue;
import com.sephrael.issueoverflow.entity.Organization;
import com.sephrael.issueoverflow.entity.Project;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class IssueFilterQueryTest extends WebIntegrationTestBase {

    private Project project;
    private User alice;
    private User bob;
    private Issue openBugByAliceForBob;
    private Issue closedTaskByAliceUnassigned;
    private Issue openBugByBobForAlice;

    private static List<Long> ids(List<Issue> issues) {
        return issues.stream().map(Issue::getId).collect(Collectors.toList());
    }

    @BeforeEach
    void createIssues() {
        Organization organization = createOrganization();
        alice = createUser(organization, "Project Manager");
        bob = createUser(organization, "Developer");
        project = createProject(organization, alice);
        openBugByAliceForBob = createIssue(project, alice, bob, "Bug", "Open", "High", 1);
        closedTaskByAliceUnassigned = createIssue(project, alice, null, "Task", "Closed", "Low", 2);
        openBugByBobForAlice = createIssue(project, bob, alice, "Bug", "Open", "Low", 3);
    }

    // Used by the issue filters when a specific assignee (or none) is chosen.
    @Test
    void assigneeFilterTreatsNullParametersAsWildcards() {
        assertThat(ids(issueRepository.findByProjectAndStatusAndPriorityAndTypeAndAssignedToAndUser(project, null, null, null, null, null)))
                .containsExactlyInAnyOrder(openBugByAliceForBob.getId(), closedTaskByAliceUnassigned.getId(), openBugByBobForAlice.getId());
        assertThat(ids(issueRepository.findByProjectAndStatusAndPriorityAndTypeAndAssignedToAndUser(project, "Open", null, null, null, null)))
                .containsExactlyInAnyOrder(openBugByAliceForBob.getId(), openBugByBobForAlice.getId());
        assertThat(ids(issueRepository.findByProjectAndStatusAndPriorityAndTypeAndAssignedToAndUser(project, null, null, "Bug", null, alice)))
                .containsExactly(openBugByAliceForBob.getId());
        assertThat(ids(issueRepository.findByProjectAndStatusAndPriorityAndTypeAndAssignedToAndUser(project, null, null, null, bob, null)))
                .containsExactly(openBugByAliceForBob.getId());
        assertThat(ids(issueRepository.findByProjectAndStatusAndPriorityAndTypeAndAssignedToAndUser(project, "Closed", "High", null, null, null)))
                .isEmpty();
    }

    // Used when "Unassigned" is chosen: IssueService passes a null assignee, so
    // "(i.assignedTo is null or i.assignedTo = :assignedTo)" returns unassigned issues.
    @Test
    void unassignedFilterReturnsUnassignedIssues() {
        assertThat(ids(issueRepository.findByProjectAndStatusAndPriorityAndTypeAndUserAndAssignedTo(project, null, null, null, null, null)))
                .containsExactly(closedTaskByAliceUnassigned.getId());
        assertThat(ids(issueRepository.findByProjectAndStatusAndPriorityAndTypeAndUserAndAssignedTo(project, null, null, null, alice, null)))
                .containsExactly(closedTaskByAliceUnassigned.getId());
        assertThat(ids(issueRepository.findByProjectAndStatusAndPriorityAndTypeAndUserAndAssignedTo(project, null, null, null, bob, null)))
                .isEmpty();
        assertThat(ids(issueRepository.findByProjectAndStatusAndPriorityAndTypeAndUserAndAssignedTo(project, null, null, null, null, bob)))
                .containsExactlyInAnyOrder(openBugByAliceForBob.getId(), closedTaskByAliceUnassigned.getId());
    }
}
