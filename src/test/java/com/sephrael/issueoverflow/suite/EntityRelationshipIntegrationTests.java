package com.sephrael.issueoverflow.suite;

import com.sephrael.issueoverflow.entity.Comment;
import com.sephrael.issueoverflow.entity.Issue;
import com.sephrael.issueoverflow.entity.Organization;
import com.sephrael.issueoverflow.entity.Project;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.repository.CommentRepository;
import com.sephrael.issueoverflow.repository.IssueRepository;
import com.sephrael.issueoverflow.repository.OrganizationRepository;
import com.sephrael.issueoverflow.repository.ProjectRepository;
import com.sephrael.issueoverflow.repository.UserRepository;
import com.sephrael.issueoverflow.support.RepositoryTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class EntityRelationshipIntegrationTests extends RepositoryTestBase {

    private static final String EMAIL = "user1@example.com";
    private static final String ORGANIZATION_NAME = "Pied Piper";
    private static final String ACCESS_KEY = "PP";
    private static final String PROJECT_NAME = "Compression";
    private static final String IDENTIFIER = "SV";
    private static final String ISSUE_TITLE = "Data Schema Overwritten";
    private static final String COMMENT_MESSAGE = "The Carver should double-check before merging";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private IssueRepository issueRepository;
    @Autowired
    private CommentRepository commentRepository;

    private User savedUser() {
        return userRepository.save(newUser(EMAIL, "Ravi", "Kumar"));
    }

    private Organization savedOrganization() {
        return organizationRepository.save(newOrganization(ORGANIZATION_NAME, ACCESS_KEY));
    }

    private Project savedProject(Organization organization) {
        return projectRepository.save(newProject(organization, PROJECT_NAME, IDENTIFIER));
    }

    private Issue savedIssue() {
        return issueRepository.save(newIssue(ISSUE_TITLE, "The Carver has accidentally overwritten our Data Schema"));
    }

    private Comment savedComment() {
        return commentRepository.save(newComment(COMMENT_MESSAGE));
    }

    private Project findProject() {
        return projectRepository.findByIdentifierAndOrganization(IDENTIFIER,
                organizationRepository.findByAccessKey(ACCESS_KEY));
    }

    @Test
    void testCreateUser() {
        User user = savedUser();
        flushAndClear();

        assertThat(entityManager.find(User.class, user.getId()).getEmail()).isEqualTo(EMAIL);
    }

    @Test
    void testCreateOrganization() {
        Organization organization = savedOrganization();
        flushAndClear();

        assertThat(entityManager.find(Organization.class, organization.getId()).getName()).isEqualTo(ORGANIZATION_NAME);
    }

    @Test
    void testCreateProject() {
        Project project = savedProject(savedOrganization());
        flushAndClear();

        assertThat(entityManager.find(Project.class, project.getId()).getName()).isEqualTo(PROJECT_NAME);
    }

    @Test
    void testCreateIssue() {
        Issue issue = savedIssue();
        flushAndClear();

        assertThat(entityManager.find(Issue.class, issue.getId()).getTitle()).isEqualTo(ISSUE_TITLE);
    }

    @Test
    void testCreateComment() {
        Comment comment = savedComment();
        flushAndClear();

        assertThat(entityManager.find(Comment.class, comment.getId()).getMessage()).isEqualTo(COMMENT_MESSAGE);
    }

    @Test
    void testUserToOrganizationConnection() {
        Organization organization = savedOrganization();
        User user = savedUser();
        user.setOrganization(organization);
        userRepository.save(user);
        flushAndClear();

        assertThat(userRepository.findByEmail(EMAIL).getOrganization().getName()).isEqualTo(ORGANIZATION_NAME);
    }

    @Test
    void testOrganizationToProjectConnection() {
        savedProject(savedOrganization());
        flushAndClear();

        assertThat(findProject().getOrganization().getName()).isEqualTo(ORGANIZATION_NAME);
    }

    @Test
    void testUserToProjectConnection() {
        Project project = savedProject(savedOrganization());
        project.addUser(savedUser());
        projectRepository.save(project);
        flushAndClear();

        assertThat(findProject().getUsers())
                .extracting(User::getEmail)
                .containsExactly(EMAIL);
    }

    @Test
    void testProjectToIssueConnection() {
        Project project = savedProject(savedOrganization());
        Issue issue = savedIssue();
        issue.setProject(project);
        issueRepository.save(issue);
        flushAndClear();

        assertThat(issueRepository.findIssueByTitle(ISSUE_TITLE).getProject().getIdentifier()).isEqualTo(IDENTIFIER);
    }

    // Issue.user (the author) is not updatable, so it must be set before the first save, as the app does when an issue is created.
    @Test
    void testUserToIssueConnection() {
        Issue issue = newIssue(ISSUE_TITLE, "The Carver has accidentally overwritten our Data Schema");
        issue.setUser(savedUser());
        issueRepository.save(issue);
        flushAndClear();

        assertThat(issueRepository.findIssueByTitle(ISSUE_TITLE).getUser().getEmail()).isEqualTo(EMAIL);
    }

    // Comment.user (the author) is not updatable, so it must be set before the first save, as the app does when a comment is posted.
    @Test
    void testUserToCommentConnection() {
        Comment comment = newComment(COMMENT_MESSAGE);
        comment.setUser(savedUser());
        commentRepository.save(comment);
        flushAndClear();

        assertThat(commentRepository.findCommentByMessage(COMMENT_MESSAGE).getUser().getEmail()).isEqualTo(EMAIL);
    }

    @Test
    void testIssueToCommentConnection() {
        Issue issue = savedIssue();
        Comment comment = savedComment();
        comment.setIssue(issue);
        commentRepository.save(comment);
        flushAndClear();

        assertThat(commentRepository.findCommentByMessage(COMMENT_MESSAGE).getIssue().getTitle()).isEqualTo(ISSUE_TITLE);
    }

    // Deletes a comment that belongs to a user and an issue. Only the comment row goes; the user and the issue stay.
    @Test
    void testDeleteComment() {
        Comment comment = newComment(COMMENT_MESSAGE);
        comment.setUser(savedUser());
        comment.setIssue(savedIssue());
        commentRepository.save(comment);
        flushAndClear();

        commentRepository.deleteById(comment.getId());
        flushAndClear();

        assertThat(commentRepository.findCommentByMessage(COMMENT_MESSAGE)).isNull();
    }

    // Deletes an issue that belongs to a project and has an author.
    @Test
    void testDeleteIssue() {
        Issue issue = newIssue(ISSUE_TITLE, "The Carver has accidentally overwritten our Data Schema");
        issue.setProject(savedProject(savedOrganization()));
        issue.setUser(savedUser());
        issueRepository.save(issue);
        flushAndClear();

        issueRepository.deleteById(issue.getId());
        flushAndClear();

        assertThat(issueRepository.findIssueByTitle(ISSUE_TITLE)).isNull();
    }

    // Deletes a project that has a member. Its project_user row goes with it, so the delete doesn't fail on the join table's foreign key.
    @Test
    void testDeleteProject() {
        Project project = savedProject(savedOrganization());
        project.addUser(savedUser());
        projectRepository.save(project);
        flushAndClear();

        projectRepository.deleteById(project.getId());
        flushAndClear();

        assertThat(findProject()).isNull();
    }

    // Deletes a user who belongs to an organization.
    @Test
    void testDeleteUser() {
        User user = savedUser();
        user.setOrganization(savedOrganization());
        userRepository.save(user);
        flushAndClear();

        userRepository.deleteById(user.getId());
        flushAndClear();

        assertThat(userRepository.findByEmail(EMAIL)).isNull();
    }

    // Deletes an organization with no users or projects. That is the state the old suite reached by its last test, after removing everything else.
    @Test
    void testDeleteOrganization() {
        Organization organization = savedOrganization();
        flushAndClear();

        organizationRepository.deleteById(organization.getId());
        flushAndClear();

        assertThat(organizationRepository.findByAccessKey(ACCESS_KEY)).isNull();
    }
}
