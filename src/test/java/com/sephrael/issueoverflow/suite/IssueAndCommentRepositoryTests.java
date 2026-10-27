package com.sephrael.issueoverflow.suite;

import com.sephrael.issueoverflow.entity.Comment;
import com.sephrael.issueoverflow.entity.Issue;
import com.sephrael.issueoverflow.repository.CommentRepository;
import com.sephrael.issueoverflow.repository.IssueRepository;
import com.sephrael.issueoverflow.support.RepositoryTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class IssueAndCommentRepositoryTests extends RepositoryTestBase {

    private static final String ISSUE_TITLE = "Headshot Not Working Properly";
    private static final String COMMENT_MESSAGE = "Please add steps to reproduce";

    @Autowired
    private IssueRepository issueRepository;
    @Autowired
    private CommentRepository commentRepository;

    private Issue savedIssue() {
        return issueRepository.save(newIssue(ISSUE_TITLE, "Shooting an enemy in the head is not affecting the enemy's damage taken"));
    }

    private Comment savedComment() {
        return commentRepository.save(newComment(COMMENT_MESSAGE));
    }

    private void saveIssueWithComment() {
        Issue issue = savedIssue();
        Comment comment = savedComment();
        comment.setIssue(issue);
        commentRepository.save(comment);
        flushAndClear();
    }

    @Test
    void testCreateIssue() {
        Issue issue = savedIssue();
        flushAndClear();

        Issue existingIssue = entityManager.find(Issue.class, issue.getId());

        assert existingIssue != null;
        assertThat(existingIssue.getTitle()).isEqualTo(ISSUE_TITLE);
    }

    @Test
    void testFindIssueByTitle() {
        savedIssue();
        flushAndClear();

        assertThat(issueRepository.findIssueByTitle(ISSUE_TITLE).getType()).isEqualTo("Bug");
    }

    @Test
    void testListIssues() {
        Issue issue = savedIssue();
        flushAndClear();

        assertThat(issueRepository.findAll())
                .extracting(Issue::getId)
                .contains(issue.getId());
    }

    @Test
    void testUpdateIssue() {
        savedIssue();
        flushAndClear();

        Issue issue = issueRepository.findIssueByTitle(ISSUE_TITLE);
        issue.setDescription("Changed Description");
        issueRepository.save(issue);
        flushAndClear();

        assertThat(issueRepository.findIssueByTitle(ISSUE_TITLE).getDescription())
                .isEqualTo("Changed Description");
    }

    @Test
    void testCreateComment() {
        Comment comment = savedComment();
        flushAndClear();

        Comment existingComment = entityManager.find(Comment.class, comment.getId());

        assert existingComment != null;
        assertThat(existingComment.getMessage()).isEqualTo(COMMENT_MESSAGE);
    }

    @Test
    void testFindCommentByMessage() {
        Comment comment = savedComment();
        flushAndClear();

        assertThat(commentRepository.findCommentByMessage(COMMENT_MESSAGE).getId()).isEqualTo(comment.getId());
    }

    @Test
    void testListComments() {
        Comment comment = savedComment();
        flushAndClear();

        assertThat(commentRepository.findAll())
                .extracting(Comment::getId)
                .contains(comment.getId());
    }

    @Test
    void testUpdateComment() {
        savedComment();
        flushAndClear();

        Comment comment = commentRepository.findCommentByMessage(COMMENT_MESSAGE);
        comment.setIsEdited(true);
        commentRepository.save(comment);
        flushAndClear();

        assertThat(commentRepository.findCommentByMessage(COMMENT_MESSAGE).isEdited()).isTrue();
    }

    @Test
    void testAddCommentToIssue() {
        saveIssueWithComment();

        Comment commentWithIssue = commentRepository.findCommentByMessage(COMMENT_MESSAGE);

        assertThat(commentWithIssue.getIssue().getTitle()).isEqualTo(ISSUE_TITLE);
    }

    @Test
    void testDeleteIssueWithAComment() {
        saveIssueWithComment();

        issueRepository.deleteById(issueRepository.findIssueByTitle(ISSUE_TITLE).getId());
        flushAndClear();

        assertThat(issueRepository.findIssueByTitle(ISSUE_TITLE)).isNull();
    }

    @Test
    void testCommentIsAlsoDeletedWhenParentIssueHasBeenDeleted() {
        saveIssueWithComment();

        issueRepository.deleteById(issueRepository.findIssueByTitle(ISSUE_TITLE).getId());
        flushAndClear();

        assertThat(commentRepository.findCommentByMessage(COMMENT_MESSAGE)).isNull();
    }

    @Test
    void testDeleteIssueWithoutComment() {
        Issue issue = savedIssue();
        flushAndClear();

        issueRepository.deleteById(issue.getId());
        flushAndClear();

        assertThat(issueRepository.findIssueByTitle(ISSUE_TITLE)).isNull();
    }
}
