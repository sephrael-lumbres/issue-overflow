package com.sephrael.issueoverflow.controller;

import com.sephrael.issueoverflow.controller.form.CommentForm;
import com.sephrael.issueoverflow.entity.Comment;
import com.sephrael.issueoverflow.entity.Issue;
import com.sephrael.issueoverflow.entity.Project;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.repository.CommentRepository;
import com.sephrael.issueoverflow.repository.IssueRepository;
import com.sephrael.issueoverflow.repository.ProjectRepository;
import com.sephrael.issueoverflow.repository.UserRepository;
import com.sephrael.issueoverflow.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Objects;

@Controller
@RequestMapping("/issues/{identifier}/view/{issueKey}/comment")
public class CommentController {
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private IssueRepository issueRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private ProjectRepository projectRepository;

    @PostMapping("/new")
    public String addComment(@PathVariable(value = "issueKey") String issueKey, CommentForm form, Principal principal,
                             @PathVariable String identifier, HttpServletRequest request) throws MessagingException, UnsupportedEncodingException {
        User currentUser = userRepository.findByEmail(principal.getName());

        if(currentUser.getOrganization() == null)
            return "organization/select-organization";

        Issue issue = findIssueOrNotFound(currentUser, identifier, issueKey);
        Comment comment = new Comment();
        comment.setMessage(form.message());
        currentUser.addToComment(comment);
        issue.addToComment(comment);

        comment.setIsEdited(false);
        commentRepository.save(comment);

        userService.sendEmailNotification(issue, comment, request, false, true);

        return "redirect:/issues/{identifier}/view/" + issueKey;
    }

    @PostMapping("/save")
    public String editComment(@RequestParam("id") Long id,
                              @RequestParam("isEdited") boolean isEdited, @RequestParam("message") String message,
                              @PathVariable(value = "issueKey") String issueKey,
                              @PathVariable String identifier, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());
        Project currentProject = projectRepository.findByIdentifierAndOrganization(identifier, currentUser.getOrganization());

        if(currentUser.getOrganization() == null)
            return "organization/select-organization";

        Issue issue = findIssueOrNotFound(currentUser, identifier, issueKey);
        Comment updatedComment = findOwnCommentOrNotFound(id, issue, currentUser);

        // sets Comment isEdited to true if comment has not yet been edited
        if(!isEdited) { updatedComment.setIsEdited(true); }

        updatedComment.setMessage(message);

        commentRepository.save(updatedComment);

        return "redirect:/issues/{identifier}/view/" + issueKey;
    }

    @RequestMapping("/{commentId}/delete")
    public String deleteComment(@PathVariable(value = "issueKey") String issueKey,
                                @PathVariable(value = "commentId") Long commentId,
                                @PathVariable String identifier, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());

        if(currentUser.getOrganization() == null)
            return "organization/select-organization";

        Issue issue = findIssueOrNotFound(currentUser, identifier, issueKey);
        Comment comment = findOwnCommentOrNotFound(commentId, issue, currentUser);
        commentRepository.delete(comment);

        return "redirect:/issues/{identifier}/view/" + issueKey;
    }

    // The issue named in the URL: in the caller's organization and in the URL's project, which the caller must be a member of.
    private Issue findIssueOrNotFound(User currentUser, String identifier, String issueKey) {
        Project currentProject = projectRepository.findByIdentifierAndOrganization(identifier, currentUser.getOrganization());

        // checks if the Current User is involved with the requested Project
        if(currentProject == null || !currentUser.getProjects().contains(currentProject))
            throw OrganizationScope.notFound();

        Issue issue = issueRepository.findByIssueKeyAndOrganization(issueKey, currentUser.getOrganization());

        if(issue == null || !Objects.equals(issue.getProject().getId(), currentProject.getId()))
            throw OrganizationScope.notFound();

        return issue;
    }

    // A comment on that issue, written by the caller (the page offers edit and delete only to the author).
    private Comment findOwnCommentOrNotFound(Long commentId, Issue issue, User currentUser) {
        Comment comment = commentId == null ? null : commentRepository.findCommentById(commentId);

        if(comment == null || !Objects.equals(comment.getIssue().getId(), issue.getId())
                || !Objects.equals(comment.getUser().getId(), currentUser.getId()))
            throw OrganizationScope.notFound();

        return comment;
    }

//    @GetMapping(path = "/json")
//    public @ResponseBody Iterable<Comment> getAllCommentsByIssueId(@PathVariable(value = "issueKey") String issueKey,
//                                                                   @PathVariable(name = "identifier") String identifier, Principal principal) {
//        return issueRepository.findByIssueKeyAndOrganization(issueKey,
//                userRepository.findByEmail(principal.getName()).getOrganization()).getComments();
//    }
}
