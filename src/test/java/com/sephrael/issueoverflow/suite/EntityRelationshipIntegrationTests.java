package com.sephrael.issueoverflow.suite;

import com.sephrael.issueoverflow.entity.*;
import com.sephrael.issueoverflow.repository.*;
import com.sephrael.issueoverflow.support.PostgresTestBase;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.annotation.Rollback;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(value = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EntityRelationshipIntegrationTests extends PostgresTestBase {

    @Autowired
    private TestEntityManager entityManager;

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

    @Test
    @Order(1)
    public void testCreateUser() {
        User user = new User();
        user.setEmail("user1@example.com");
        user.setPassword("test-password-1");
        user.setFirstName("Ravi");
        user.setLastName("Kumar");

        User savedUser = userRepository.save(user);

        User existingUser = entityManager.find(User.class, savedUser.getId());

        assertThat(user.getEmail()).isEqualTo(existingUser.getEmail());
    }

    @Test
    @Order(2)
    public void testCreateOrganization() {
        Organization organization = new Organization();
        organization.setName("Pied Piper");
        organization.setAccessKey("PP");

        Organization savedOrganization = organizationRepository.save(organization);

        Organization existingOrganization = entityManager.find(Organization.class, savedOrganization.getId());

        assertThat(organization.getName()).isEqualTo(existingOrganization.getName());
    }

    @Test
    @Order(3)
    public void testCreateProject() {
        Project project = new Project();
        project.setName("Compression");
        project.setIdentifier("SV");
        project.setOrganization(organizationRepository.findByAccessKey("PP"));

        Project savedProject = projectRepository.save(project);

        Project existingProject = entityManager.find(Project.class, savedProject.getId());

        assertThat(project.getName()).isEqualTo(existingProject.getName());
    }

    @Test
    @Order(4)
    public void testCreateIssue() {
        Issue issue = new Issue();
        issue.setTitle("Data Schema Overwritten");
        issue.setDescription("The Carver has accidentally overwritten our Data Schema");
        issue.setType("Bug");
        issue.setPriority("High");
        issue.setStatus("Open");


        Issue savedIssue = issueRepository.save(issue);

        Issue existingIssue = entityManager.find(Issue.class, savedIssue.getId());

        assertThat(issue.getTitle()).isEqualTo(existingIssue.getTitle());
    }

    @Test
    @Order(5)
    public void testCreateComment() {
        Comment comment = new Comment();
        comment.setMessage("The Carver should double-check before merging");

        Comment savedComment = commentRepository.save(comment);

        Comment existingComment = entityManager.find(Comment.class, savedComment.getId());

        assertThat(comment.getMessage()).isEqualTo(existingComment.getMessage());
    }

    @Test
    @Order(6)
    public void testUserToOrganizationConnection() {
        User user = userRepository.findByEmail("user1@example.com");
        user.setOrganization(organizationRepository.findByAccessKey("PP"));

        userRepository.save(user);

        User userWithOrganization = userRepository.findByEmail("user1@example.com");

        assertThat(userWithOrganization.getOrganization().getName()).isEqualTo("Pied Piper");
    }

    @Test
    @Order(7)
    public void testOrganizationToProjectConnection() {
        Project project = projectRepository.findByIdentifierAndOrganization("SV",
                organizationRepository.findByAccessKey("PP"));

        projectRepository.save(project);

        Project projectWithOrganization = projectRepository.findByIdentifierAndOrganization("SV",
                organizationRepository.findByAccessKey("PP"));

        assertThat(projectWithOrganization.getOrganization().getName()).isEqualTo("Pied Piper");
    }

    @Test
    @Order(8)
    public void testUserToProjectConnection() {
        Project project = projectRepository.findByIdentifierAndOrganization("SV",
                organizationRepository.findByAccessKey("PP"));
        project.addUser(userRepository.findByEmail("user1@example.com"));

        projectRepository.save(project);

        Project projectWithUser = projectRepository.findByIdentifierAndOrganization("SV",
                organizationRepository.findByAccessKey("PP"));

        assertThat(projectWithUser.getUsers().get(0).getEmail()).isEqualTo("user1@example.com");
    }

    @Test
    @Order(9)
    public void testProjectToIssueConnection() {
        Issue issue = issueRepository.findIssueByTitle("Data Schema Overwritten");
        issue.setProject(projectRepository.findByIdentifierAndOrganization("SV",
                organizationRepository.findByAccessKey("PP")));

        issueRepository.save(issue);

        Issue issueWithProject = issueRepository.findIssueByTitle("Data Schema Overwritten");

        assertThat(issueWithProject.getTitle()).isEqualTo("Data Schema Overwritten");
    }

    @Test
    @Order(10)
    public void testUserToIssueConnection() {
        Issue issue = issueRepository.findIssueByTitle("Data Schema Overwritten");
        issue.setUser(userRepository.findByEmail("user1@example.com"));

        issueRepository.save(issue);

        Issue issueWithUser = issueRepository.findIssueByTitle("Data Schema Overwritten");

        assertThat(issueWithUser.getUser().getEmail()).isEqualTo("user1@example.com");
    }

    @Test
    @Order(11)
    public void testUserToCommentConnection() {
        Comment comment = commentRepository.findCommentByMessage("The Carver should double-check before merging");
        comment.setUser(userRepository.findByEmail("user1@example.com"));

        commentRepository.save(comment);

        Comment commentWithUser = commentRepository.findCommentByMessage("The Carver should double-check before merging");

        assertThat(commentWithUser.getUser().getEmail()).isEqualTo("user1@example.com");
    }

    @Test
    @Order(12)
    public void testIssueToCommentConnection() {
        Comment comment = commentRepository.findCommentByMessage("The Carver should double-check before merging");
        comment.setIssue(issueRepository.findIssueByTitle("Data Schema Overwritten"));

        commentRepository.save(comment);

        Comment commentWithIssue = commentRepository.findCommentByMessage("The Carver should double-check before merging");

        assertThat(commentWithIssue.getIssue().getTitle()).isEqualTo("Data Schema Overwritten");
    }

    @Test
    @Order(13)
    public void testDeleteComment() {
        Comment comment = commentRepository.findCommentByMessage("The Carver should double-check before merging");

        commentRepository.deleteById(comment.getId());

        Comment deletedComment = commentRepository.findCommentByMessage("The Carver should double-check before merging");

        assertThat(deletedComment).isNull();
    }

    @Test
    @Order(14)
    public void testDeleteIssue() {
        Issue issue = issueRepository.findIssueByTitle("Data Schema Overwritten");

        issueRepository.deleteById(issue.getId());

        Issue deletedIssue = issueRepository.findIssueByTitle("Data Schema Overwritten");

        assertThat(deletedIssue).isNull();
    }

    @Test
    @Order(15)
    public void testDeleteProject() {
        Project project = projectRepository.findByIdentifierAndOrganization("SV",
                organizationRepository.findByAccessKey("PP"));

        projectRepository.deleteById(project.getId());

        Project deletedProject = projectRepository.findByIdentifierAndOrganization("SV",
                organizationRepository.findByAccessKey("PP"));

        assertThat(deletedProject).isNull();
    }

    @Test
    @Order(16)
    public void testDeleteUser() {
        User user = userRepository.findByEmail("user1@example.com");

        userRepository.deleteById(user.getId());

        User deletedUser = userRepository.findByEmail("user1@example.com");

        assertThat(deletedUser).isNull();
    }

    @Test
    @Order(17)
    public void testDeleteOrganization() {
        Organization organization = organizationRepository.findByAccessKey("PP");

        organizationRepository.deleteById(organization.getId());

        Organization deletedOrganization = organizationRepository.findByAccessKey("PP");

        assertThat(deletedOrganization).isNull();
    }
}
