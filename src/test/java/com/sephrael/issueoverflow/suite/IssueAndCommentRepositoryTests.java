package com.sephrael.issueoverflow.suite;

import com.sephrael.issueoverflow.entity.Comment;
import com.sephrael.issueoverflow.entity.Issue;
import com.sephrael.issueoverflow.repository.CommentRepository;
import com.sephrael.issueoverflow.repository.IssueRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(value = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IssueAndCommentRepositoryTests extends PostgresTestBase {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private IssueRepository issueRepository;
    @Autowired
    private CommentRepository commentRepository;

    @Test
    @Order(1)
    public void testCreateIssue() {
        Issue issue = new Issue();
        issue.setTitle("Headshot Not Working Properly");
        issue.setDescription("Shooting an enemy in the head is not affecting the enemy's damage taken");
        issue.setType("Bug");
        issue.setPriority("High");
        issue.setStatus("Open");

        Issue savedIssue = issueRepository.save(issue);

        Issue existingIssue = entityManager.find(Issue.class, savedIssue.getId());

        assertThat(issue.getTitle()).isEqualTo(existingIssue.getTitle());
    }

    @Test
    @Order(2)
    public void testFindIssueByTitle() {
        Issue issue = issueRepository.findIssueByTitle("Headshot Not Working Properly");
        assertThat(issue.getTitle()).isEqualTo("Headshot Not Working Properly");
    }

    @Test
    @Order(3)
    public void testListIssues() {
        List<Issue> issues = (List<Issue>) issueRepository.findAll();
        assertThat(issues).size().isGreaterThan(0);
    }

    @Test
    @Order(4)
    public void testUpdateIssue() {
        Issue issue = issueRepository.findIssueByTitle("Headshot Not Working Properly");
        issue.setDescription("Changed Description");

        issueRepository.save(issue);

        Issue updatedIssue = issueRepository.findIssueByTitle("Headshot Not Working Properly");

        assertThat(updatedIssue.getDescription()).isEqualTo("Changed Description");
    }

    @Test
    @Order(5)
    public void testCreateComment() {
        Comment comment = new Comment();
        comment.setMessage("Please add steps to reproduce");

        Comment savedComment = commentRepository.save(comment);

        Comment existingComment = entityManager.find(Comment.class, savedComment.getId());

        assertThat(comment.getMessage()).isEqualTo(existingComment.getMessage());
    }

    @Test
    @Order(6)
    public void testFindCommentByMessage() {
        Comment comment = commentRepository.findCommentByMessage("Please add steps to reproduce");
        assertThat(comment.getMessage()).isEqualTo("Please add steps to reproduce");
    }

    @Test
    @Order(7)
    public void testListComments() {
        List<Comment> comments = (List<Comment>) commentRepository.findAll();
        assertThat(comments).size().isGreaterThan(0);
    }

    @Test
    @Order(8)
    public void testUpdateComment() {
        Comment comment = commentRepository.findCommentByMessage("Please add steps to reproduce");
        comment.setIsEdited(true);

        commentRepository.save(comment);

        Comment updatedComment = commentRepository.findCommentByMessage("Please add steps to reproduce");

        assertThat(updatedComment.isEdited()).isEqualTo(true);
    }

    @Test
    @Order(9)
    public void testAddCommentToIssue() {
        Issue issue = issueRepository.findIssueByTitle("Headshot Not Working Properly");
        Comment comment = commentRepository.findCommentByMessage("Please add steps to reproduce");

        comment.setIssue(issue);
        commentRepository.save(comment);

        Comment commentWithIssue = commentRepository.findCommentByMessage("Please add steps to reproduce");

        assertThat(commentWithIssue.getIssue().getTitle()).isEqualTo("Headshot Not Working Properly");
    }

    @Test
    @Order(10)
    public void testDeleteIssueWithAComment() {
        Issue issue = issueRepository.findIssueByTitle("Headshot Not Working Properly");

        issueRepository.deleteById(issue.getId());

        Issue deletedIssue = issueRepository.findIssueByTitle("Headshot Not Working Properly");

        assertThat(deletedIssue).isNull();
    }

    @Test
    @Order(11)
    public void testCommentIsAlsoDeletedWhenParentIssueHasBeenDeleted() {
        Comment deletedComment = commentRepository.findCommentByMessage("Please add steps to reproduce");

        assertThat(deletedComment).isNull();
    }

    @Test
    @Order(12)
    public void testDeleteIssueWithoutComment() {
        testCreateIssue();
        Issue issue = issueRepository.findIssueByTitle("Headshot Not Working Properly");

        issueRepository.deleteById(issue.getId());

        Issue deletedIssue = issueRepository.findIssueByTitle("Headshot Not Working Properly");

        assertThat(deletedIssue).isNull();
    }
}
