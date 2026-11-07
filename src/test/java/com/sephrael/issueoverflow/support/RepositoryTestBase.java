package com.sephrael.issueoverflow.support;

import com.sephrael.issueoverflow.entity.Comment;
import com.sephrael.issueoverflow.entity.Issue;
import com.sephrael.issueoverflow.entity.Organization;
import com.sephrael.issueoverflow.entity.Project;
import com.sephrael.issueoverflow.entity.Role;
import com.sephrael.issueoverflow.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Base class for the repository tests.
 * <p>
 * Every test runs in its own transaction, which is rolled back afterward, and
 * creates the data it needs. Nothing is committed, so the order in which tests
 * and classes run does not matter. Subclasses must not add Spring test
 * annotations, so that they all share one Spring context.
 * <p>
 * Call flushAndClear() between arranging data and checking it: it writes the
 * pending SQL and empties the persistence context, so the next read and any
 * cascading delete see what is in the database, as they would in a new request.
 * <p>
 * An issue needs a project and an author, and a comment needs an issue and
 * an author (NOT NULL columns). newIssue() and newComment() fill these with
 * placeholders, which are saved the first time a test needs them. A test can
 * set its own values before saving.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class RepositoryTestBase extends PostgresTestBase {

    @Autowired
    protected TestEntityManager entityManager;

    private User placeholderAuthor;
    private Project placeholderProject;
    private Issue placeholderIssue;

    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    protected static Organization newOrganization(String name, String accessKey) {
        Organization organization = new Organization();
        organization.setName(name);
        organization.setAccessKey(accessKey);
        return organization;
    }

    protected static Project newProject(Organization organization, String name, String identifier) {
        Project project = new Project();
        project.setName(name);
        project.setIdentifier(identifier);
        project.setColor("primary");
        project.setOrganization(organization);
        return project;
    }

    // An Open, High priority Bug in the placeholder project, by the placeholder author, with no organization.
    protected Issue newIssue(String title, String description) {
        Issue issue = new Issue();
        issue.setTitle(title);
        issue.setDescription(description);
        issue.setType("Bug");
        issue.setPriority("High");
        issue.setStatus("Open");
        issue.setProject(placeholderProject());
        issue.setUser(placeholderAuthor());
        return issue;
    }

    // A comment on the placeholder issue, by the placeholder author.
    protected Comment newComment(String message) {
        Comment comment = new Comment();
        comment.setMessage(message);
        comment.setIssue(placeholderIssue());
        comment.setUser(placeholderAuthor());
        return comment;
    }

    protected static User newUser(String email, String firstName, String lastName) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("test-password-1");
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return user;
    }

    protected static Role newRole(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }

    private User placeholderAuthor() {
        if(placeholderAuthor == null)
            placeholderAuthor = entityManager.persist(newUser("placeholder-author@example.test", "Placeholder", "Author"));
        return placeholderAuthor;
    }

    private Project placeholderProject() {
        if(placeholderProject == null) {
            Organization organization = entityManager.persist(newOrganization("Placeholder Organization", "PLACEHOLDER"));
            placeholderProject = entityManager.persist(newProject(organization, "Placeholder Project", "PH"));
        }
        return placeholderProject;
    }

    private Issue placeholderIssue() {
        if(placeholderIssue == null) {
            Issue issue = new Issue();
            issue.setTitle("Placeholder issue");
            issue.setProject(placeholderProject());
            issue.setUser(placeholderAuthor());
            placeholderIssue = entityManager.persist(issue);
        }
        return placeholderIssue;
    }
}
