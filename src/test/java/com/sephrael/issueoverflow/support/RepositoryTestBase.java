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
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class RepositoryTestBase extends PostgresTestBase {

    @Autowired
    protected TestEntityManager entityManager;

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
        project.setOrganization(organization);
        return project;
    }

    // An Open, High priority Bug with no project, author or organization.
    protected static Issue newIssue(String title, String description) {
        Issue issue = new Issue();
        issue.setTitle(title);
        issue.setDescription(description);
        issue.setType("Bug");
        issue.setPriority("High");
        issue.setStatus("Open");
        return issue;
    }

    protected static Comment newComment(String message) {
        Comment comment = new Comment();
        comment.setMessage(message);
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
}
