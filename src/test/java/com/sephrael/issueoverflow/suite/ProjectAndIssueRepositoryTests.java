package com.sephrael.issueoverflow.suite;

import com.sephrael.issueoverflow.entity.Comment;
import com.sephrael.issueoverflow.entity.Issue;
import com.sephrael.issueoverflow.entity.Organization;
import com.sephrael.issueoverflow.entity.Project;
import com.sephrael.issueoverflow.repository.CommentRepository;
import com.sephrael.issueoverflow.repository.IssueRepository;
import com.sephrael.issueoverflow.repository.OrganizationRepository;
import com.sephrael.issueoverflow.repository.ProjectRepository;
import com.sephrael.issueoverflow.support.RepositoryTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectAndIssueRepositoryTests extends RepositoryTestBase {

    private static final String ACCESS_KEY = "2K";
    private static final String ISSUE_TITLE = "Wonky Dunk Animations";
    private static final String COMMENT_MESSAGE = "This bug is very hard to recreate";

    @Autowired
    private IssueRepository issueRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private OrganizationRepository organizationRepository;

    private Organization savedOrganization() {
        return organizationRepository.save(newOrganization("2K Games", ACCESS_KEY));
    }

    // The organization with the "NBA 2K" (NBA) and "WWE 2K" (WWE) projects.
    private void saveTwoProjects() {
        Organization organization = savedOrganization();
        projectRepository.save(newProject(organization, "NBA 2K", "NBA"));
        projectRepository.save(newProject(organization, "WWE 2K", "WWE"));
    }

    // Both projects, plus the issue in NBA with one comment.
    private void saveIssueWithCommentInFirstProject() {
        saveTwoProjects();
        Issue issue = issueRepository.save(newIssue(ISSUE_TITLE, "When a player performs a dunk, the character sprite is upside down"));
        issue.setProject(findProject("NBA"));
        issueRepository.save(issue);
        Comment comment = commentRepository.save(newComment(COMMENT_MESSAGE));
        comment.setIssue(issue);
        commentRepository.save(comment);
        flushAndClear();
    }

    private Project findProject(String identifier) {
        return projectRepository.findByIdentifierAndOrganization(identifier, organizationRepository.findByAccessKey(ACCESS_KEY));
    }

    @Test
    void testCreateOrganization() {
        Organization organization = savedOrganization();
        flushAndClear();

        assertThat(entityManager.find(Organization.class, organization.getId()).getName()).isEqualTo("2K Games");
    }

    @Test
    void testCreateTwoProjects() {
        Organization organization = savedOrganization();
        Project project1 = projectRepository.save(newProject(organization, "NBA 2K", "NBA"));
        Project project2 = projectRepository.save(newProject(organization, "WWE 2K", "WWE"));
        flushAndClear();

        assertThat(entityManager.find(Project.class, project1.getId()).getName()).isEqualTo("NBA 2K");
        assertThat(entityManager.find(Project.class, project2.getId()).getName()).isEqualTo("WWE 2K");
    }

    @Test
    void testFindProjectsByIdentifier() {
        saveTwoProjects();
        flushAndClear();

        assertThat(findProject("NBA").getIdentifier()).isEqualTo("NBA");
        assertThat(findProject("WWE").getIdentifier()).isEqualTo("WWE");
    }

    @Test
    void testListProjects() {
        saveTwoProjects();
        flushAndClear();

        assertThat(projectRepository.findAll())
                .extracting(Project::getIdentifier)
                .contains("NBA", "WWE");
    }

    @Test
    void testUpdateProjects() {
        saveTwoProjects();
        flushAndClear();

        Project project1 = findProject("NBA");
        project1.setName("NBA 2K22");
        projectRepository.save(project1);
        Project project2 = findProject("WWE");
        project2.setName("WWE 2K22");
        projectRepository.save(project2);
        flushAndClear();

        assertThat(findProject("NBA").getName()).isEqualTo("NBA 2K22");
        assertThat(findProject("WWE").getName()).isEqualTo("WWE 2K22");
    }

    @Test
    void testCreateIssue() {
        Issue issue = issueRepository.save(newIssue(ISSUE_TITLE, "When a player performs a dunk, the character sprite is upside down"));
        flushAndClear();

        assertThat(entityManager.find(Issue.class, issue.getId()).getTitle()).isEqualTo(ISSUE_TITLE);
    }

    @Test
    void testCreateComment() {
        Comment comment = commentRepository.save(newComment(COMMENT_MESSAGE));
        flushAndClear();

        assertThat(entityManager.find(Comment.class, comment.getId()).getMessage()).isEqualTo(COMMENT_MESSAGE);
    }

    @Test
    void testAddCommentToIssue() {
        saveIssueWithCommentInFirstProject();

        assertThat(commentRepository.findCommentByMessage(COMMENT_MESSAGE).getIssue().getTitle()).isEqualTo(ISSUE_TITLE);
    }

    @Test
    void testAddIssueToProject() {
        saveIssueWithCommentInFirstProject();

        assertThat(issueRepository.findIssueByTitle(ISSUE_TITLE).getProject().getIdentifier()).isEqualTo("NBA");
    }

    @Test
    void testSwitchIssueWithCommentToDifferentProject() {
        saveIssueWithCommentInFirstProject();

        Issue issue = issueRepository.findIssueByTitle(ISSUE_TITLE);
        issue.setProject(findProject("WWE"));
        issueRepository.save(issue);
        flushAndClear();

        assertThat(issueRepository.findIssueByTitle(ISSUE_TITLE).getProject().getName()).isEqualTo("WWE 2K");
        assertThat(commentRepository.findCommentByMessage(COMMENT_MESSAGE).getIssue().getTitle()).isEqualTo(ISSUE_TITLE);
    }

    @Test
    void testUpdateIssueAfterProjectSwitch() {
        saveIssueWithCommentInFirstProject();
        Issue switchedIssue = issueRepository.findIssueByTitle(ISSUE_TITLE);
        switchedIssue.setProject(findProject("WWE"));
        issueRepository.save(switchedIssue);
        flushAndClear();

        Issue issue = issueRepository.findIssueByTitle(ISSUE_TITLE);
        issue.setStatus("Closed");
        issueRepository.save(issue);
        flushAndClear();

        Issue updatedIssue = issueRepository.findIssueByTitle(ISSUE_TITLE);
        assertThat(updatedIssue.getStatus()).isEqualTo("Closed");
        assertThat(updatedIssue.getProject().getName()).isEqualTo("WWE 2K");
        assertThat(commentRepository.findCommentByMessage(COMMENT_MESSAGE).getIssue().getTitle()).isEqualTo(ISSUE_TITLE);
    }

    // As in the old ordered tests, one of the deleted projects holds an issue with a comment.
    @Test
    void testDeleteProjects() {
        saveIssueWithCommentInFirstProject();

        projectRepository.deleteById(findProject("WWE").getId());
        projectRepository.deleteById(findProject("NBA").getId());
        flushAndClear();

        assertThat(findProject("WWE")).isNull();
        assertThat(findProject("NBA")).isNull();
    }

    @Test
    void testIssueAndCommentAreDeletedWhenParentProjectHasBeenDeleted() {
        saveIssueWithCommentInFirstProject();

        projectRepository.deleteById(findProject("NBA").getId());
        flushAndClear();

        assertThat(issueRepository.findIssueByTitle(ISSUE_TITLE)).isNull();
        assertThat(commentRepository.findCommentByMessage(COMMENT_MESSAGE)).isNull();
    }

    @Test
    void testCreateProjectIssueAndCommentWithoutSwitching() {
        Organization organization = savedOrganization();
        Project project = projectRepository.save(newProject(organization, "MLB 2K", "MLB"));
        Issue issue = issueRepository.save(newIssue("Incorrect Baseball Bat Sprite", "Baseball bat sprite shows the wrong item"));
        Comment comment = commentRepository.save(newComment("I wonder who drew that sprite"));
        comment.setIssue(issue);
        commentRepository.save(comment);
        issue.setProject(project);
        issueRepository.save(issue);
        flushAndClear();

        Issue reloadedIssue = issueRepository.findIssueByTitle("Incorrect Baseball Bat Sprite");
        assertThat(reloadedIssue.getProject().getId()).isEqualTo(findProject("MLB").getId());
        assertThat(commentRepository.findCommentByMessage("I wonder who drew that sprite").getIssue().getId()).isEqualTo(reloadedIssue.getId());
    }

    @Test
    void testDeleteProjectWithoutSwitchingIssueToDifferentProject() {
        Organization organization = savedOrganization();
        Project project = projectRepository.save(newProject(organization, "MLB 2K", "MLB"));
        Issue issue = issueRepository.save(newIssue("Incorrect Baseball Bat Sprite", "Baseball bat sprite shows the wrong item"));
        issue.setProject(project);
        issueRepository.save(issue);
        Comment comment = commentRepository.save(newComment("I wonder who drew that sprite"));
        comment.setIssue(issue);
        commentRepository.save(comment);
        flushAndClear();

        projectRepository.deleteById(findProject("MLB").getId());
        flushAndClear();

        assertThat(findProject("MLB")).isNull();
        assertThat(issueRepository.findIssueByTitle("Incorrect Baseball Bat Sprite")).isNull();
        assertThat(commentRepository.findCommentByMessage("I wonder who drew that sprite")).isNull();
    }

    @Test
    void testDeleteOrganization() {
        Organization organization = savedOrganization();
        flushAndClear();

        organizationRepository.deleteById(organization.getId());
        flushAndClear();

        assertThat(organizationRepository.findByAccessKey(ACCESS_KEY)).isNull();
    }
}
