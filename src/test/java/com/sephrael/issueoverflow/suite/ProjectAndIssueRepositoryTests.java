package com.sephrael.issueoverflow.suite;

import com.sephrael.issueoverflow.entity.Comment;
import com.sephrael.issueoverflow.entity.Issue;
import com.sephrael.issueoverflow.entity.Organization;
import com.sephrael.issueoverflow.entity.Project;
import com.sephrael.issueoverflow.repository.CommentRepository;
import com.sephrael.issueoverflow.repository.IssueRepository;
import com.sephrael.issueoverflow.repository.OrganizationRepository;
import com.sephrael.issueoverflow.repository.ProjectRepository;
import com.sephrael.issueoverflow.support.PostgresTestBase;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.Rollback;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(value = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProjectAndIssueRepositoryTests extends PostgresTestBase {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private IssueRepository issueRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private OrganizationRepository organizationRepository;

    @Test
    @Order(1)
    public void testCreateOrganization() {
        Organization organization = new Organization();
        organization.setName("2K Games");
        organization.setAccessKey("2K");

        Organization savedOrganization = organizationRepository.save(organization);

        Organization existingOrganization = entityManager.find(Organization.class, savedOrganization.getId());

        assertThat(organization.getName()).isEqualTo(existingOrganization.getName());
    }

    @Test
    @Order(2)
    public void testCreateTwoProjects() {
        Project project1 = new Project();
        project1.setName("NBA 2K");
        project1.setIdentifier("NBA");
        project1.setOrganization(organizationRepository.findByAccessKey("2K"));
        Project savedProject1 = projectRepository.save(project1);
        Project existingProject1 = entityManager.find(Project.class, savedProject1.getId());

        Project project2 = new Project();
        project2.setName("WWE 2K");
        project2.setIdentifier("WWE");
        project2.setOrganization(organizationRepository.findByAccessKey("2K"));
        Project savedProject2 = projectRepository.save(project2);
        Project existingProject2 = entityManager.find(Project.class, savedProject2.getId());

        assertThat(project1.getName()).isEqualTo(existingProject1.getName());
        assertThat(project2.getName()).isEqualTo(existingProject2.getName());
    }

    @Test
    @Order(3)
    public void testFindProjectsByIdentifier() {
        Project project1 = projectRepository.findByIdentifierAndOrganization("NBA",
                organizationRepository.findByAccessKey("2K"));
        assertThat(project1.getIdentifier()).isEqualTo("NBA");

        Project project2 = projectRepository.findByIdentifierAndOrganization("WWE",
                organizationRepository.findByAccessKey("2K"));
        assertThat(project2.getIdentifier()).isEqualTo("WWE");
    }

    @Test
    @Order(4)
    public void testListProjects() {
        List<Project> projects = (List<Project>) projectRepository.findAll();
        assertThat(projects).size().isGreaterThan(0);
    }

    @Test
    @Order(5)
    public void testUpdateProjects() {
        Project project1 = projectRepository.findByIdentifierAndOrganization("NBA",
                organizationRepository.findByAccessKey("2K"));
        project1.setName("NBA 2K22");
        projectRepository.save(project1);
        Project updatedProject1 = projectRepository.findByIdentifierAndOrganization("NBA",
                organizationRepository.findByAccessKey("2K"));

        Project project2 = projectRepository.findByIdentifierAndOrganization("WWE",
                organizationRepository.findByAccessKey("2K"));
        project2.setName("WWE 2K22");
        projectRepository.save(project2);
        Project updatedProject2 = projectRepository.findByIdentifierAndOrganization("WWE",
                organizationRepository.findByAccessKey("2K"));

        assertThat(updatedProject1.getName()).isEqualTo("NBA 2K22");
        assertThat(updatedProject2.getName()).isEqualTo("WWE 2K22");
    }

    @Test
    @Order(6)
    public void testCreateIssue() {
        Issue issue = new Issue();
        issue.setTitle("Wonky Dunk Animations");
        issue.setDescription("When a player performs a dunk, the character sprite is upside down");
        issue.setType("Bug");
        issue.setPriority("High");
        issue.setStatus("Open");

        Issue savedIssue = issueRepository.save(issue);

        Issue existingIssue = entityManager.find(Issue.class, savedIssue.getId());

        assertThat(issue.getTitle()).isEqualTo(existingIssue.getTitle());
    }

    @Test
    @Order(7)
    public void testCreateComment() {
        Comment comment = new Comment();
        comment.setMessage("This bug is very hard to recreate");

        Comment savedComment = commentRepository.save(comment);

        Comment existingComment = entityManager.find(Comment.class, savedComment.getId());

        assertThat(comment.getMessage()).isEqualTo(existingComment.getMessage());
    }

    @Test
    @Order(8)
    public void testAddCommentToIssue() {
        Issue issue = issueRepository.findIssueByTitle("Wonky Dunk Animations");
        Comment comment = commentRepository.findCommentByMessage("This bug is very hard to recreate");

        comment.setIssue(issue);
        commentRepository.save(comment);

        Comment commentWithIssue = commentRepository.findCommentByMessage("This bug is very hard to recreate");

        assertThat(commentWithIssue.getIssue().getTitle()).isEqualTo("Wonky Dunk Animations");
    }

    @Test
    @Order(9)
    public void testAddIssueToProject() {
        Issue issue = issueRepository.findIssueByTitle("Wonky Dunk Animations");
        issue.setProject(projectRepository.findByIdentifierAndOrganization("NBA",
                organizationRepository.findByAccessKey("2K")));

        issueRepository.save(issue);

        Issue issueWithProject = issueRepository.findIssueByTitle("Wonky Dunk Animations");

        assertThat(issueWithProject.getTitle()).isEqualTo("Wonky Dunk Animations");
    }

    @Test
    @Order(10)
    public void testSwitchIssueWithCommentToDifferentProject() {
        Issue issue = issueRepository.findIssueByTitle("Wonky Dunk Animations");
        issue.setProject(projectRepository.findByIdentifierAndOrganization("WWE",
                organizationRepository.findByAccessKey("2K")));
        Comment comment = commentRepository.findCommentByMessage("This bug is very hard to recreate");

        issueRepository.save(issue);

        Issue issueWithDifferentProject = issueRepository.findIssueByTitle("Wonky Dunk Animations");

        assertThat(issueWithDifferentProject.getProject().getName()).isEqualTo("WWE 2K22");
        assertThat(comment.getIssue().getTitle()).isEqualTo("Wonky Dunk Animations");
    }

    @Test
    @Order(11)
    public void testUpdateIssueAfterProjectSwitch() {
        Issue issue = issueRepository.findIssueByTitle("Wonky Dunk Animations");
        Comment comment = commentRepository.findCommentByMessage("This bug is very hard to recreate");
        issue.setStatus("Closed");
        issueRepository.save(issue);

        Issue updatedIssue = issueRepository.findIssueByTitle("Wonky Dunk Animations");

        assertThat(updatedIssue.getStatus()).isEqualTo("Closed");
        assertThat(updatedIssue.getProject().getName()).isEqualTo("WWE 2K22");
        assertThat(comment.getIssue().getTitle()).isEqualTo("Wonky Dunk Animations");
    }

    @Test
    @Order(12)
    public void testDeleteProjects() {
        Project project1 = projectRepository.findByIdentifierAndOrganization("WWE",
                organizationRepository.findByAccessKey("2K"));
        Project project2 = projectRepository.findByIdentifierAndOrganization("NBA",
                organizationRepository.findByAccessKey("2K"));

        projectRepository.deleteById(project1.getId());
        projectRepository.deleteById(project2.getId());

        Project deletedProject1 = projectRepository.findByIdentifierAndOrganization("WWE",
                organizationRepository.findByAccessKey("2K"));
        Project deletedProject2 = projectRepository.findByIdentifierAndOrganization("NBA",
                organizationRepository.findByAccessKey("2K"));

        assertThat(deletedProject1).isNull();
        assertThat(deletedProject2).isNull();
    }

    @Test
    @Order(13)
    public void testIssueAndCommentAreDeletedWhenParentProjectHasBeenDeleted() {
        Issue issue = issueRepository.findIssueByTitle("Wonky Dunk Animations");
        Comment comment = commentRepository.findCommentByMessage("This bug is very hard to recreate");

        assertThat(issue).isNull();
        assertThat(comment).isNull();
    }

    @Test
    @Order(14)
    public void testCreateProjectIssueAndCommentWithoutSwitching() {

        // creates a new project
        Project project = new Project();
        project.setName("MLB 2K");
        project.setIdentifier("MLB");
        project.setOrganization(organizationRepository.findByAccessKey("2K"));
        projectRepository.save(project);

        // creates a new issue
        Issue issue = new Issue();
        issue.setTitle("Incorrect Baseball Bat Sprite");
        issue.setDescription("Baseball bat sprite shows the wrong item");
        issue.setType("Bug");
        issue.setPriority("High");
        issue.setStatus("Open");
        issueRepository.save(issue);

        // creates a new comment
        Comment comment = new Comment();
        comment.setMessage("I wonder who drew that sprite");
        commentRepository.save(comment);

        // adds comment to issue
        comment.setIssue(issue);
        commentRepository.save(comment);

        // adds issue to project
        issue.setProject(projectRepository.findByIdentifierAndOrganization("MLB",
                organizationRepository.findByAccessKey("2K")));
        issueRepository.save(issue);

        assertThat(issue.getProject()).isEqualTo(projectRepository.findByIdentifierAndOrganization("MLB",
                organizationRepository.findByAccessKey("2K")));
    }

    @Test
    @Order(15)
    public void testDeleteProjectWithoutSwitchingIssueToDifferentProject() {
        Project project = projectRepository.findByIdentifierAndOrganization("MLB",
                organizationRepository.findByAccessKey("2K"));

        // deletes the project with the issue and comment
        projectRepository.deleteById(project.getId());
        Project deletedProject = projectRepository.findByIdentifierAndOrganization("MLB",
                organizationRepository.findByAccessKey("2K"));
        Issue deletedIssue = issueRepository.findIssueByTitle("Incorrect Baseball Bat Sprite");
        Comment deletedComment = commentRepository.findCommentByMessage("I wonder who drew that sprite");

        assertThat(deletedProject).isNull();
        assertThat(deletedIssue).isNull();
        assertThat(deletedComment).isNull();
    }

    @Test
    @Order(16)
    public void testDeleteOrganization() {
        Organization organization = organizationRepository.findByAccessKey("2K");

        organizationRepository.deleteById(organization.getId());

        Organization deletedOrganization = organizationRepository.findByAccessKey("2K");

        assertThat(deletedOrganization).isNull();
    }
}
