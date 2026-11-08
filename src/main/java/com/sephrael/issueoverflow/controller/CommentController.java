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
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Objects;

@Controller
@RequestMapping("/issues/{identifier}/view/{issueKey}/comment")
public class CommentController {

    private static final String COMMENT_ERROR = "Comments can't be empty or too long.";

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
    public String addComment(@PathVariable(value = "issueKey") String issueKey, @Valid CommentForm form, BindingResult bindingResult,
                             Principal principal, @PathVariable String identifier, RedirectAttributes redirectAttributes)
            throws MessagingException, UnsupportedEncodingException {
        User currentUser = userRepository.findByEmail(principal.getName());

        if(currentUser.getOrganization() == null)
            return "organization/select-organization";

        // the issue is looked up first, so a non-member gets a 404 whatever the form holds
        Issue issue = findIssueOrNotFound(currentUser, identifier, issueKey);

        if(bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("commentError", COMMENT_ERROR);
            return "redirect:/issues/{identifier}/view/" + issueKey;
        }

        Comment comment = new Comment();
        comment.setMessage(form.message());
        currentUser.addToComment(comment);
        issue.addToComment(comment);

        comment.setIsEdited(false);
        commentRepository.save(comment);

        userService.sendEmailNotification(issue, comment, false, true);

        return "redirect:/issues/{identifier}/view/" + issueKey;
    }

    @PostMapping("/save")
    public String editComment(@RequestParam("id") Long id,
                              @RequestParam("isEdited") boolean isEdited, @Valid CommentForm form, BindingResult bindingResult,
                              @PathVariable(value = "issueKey") String issueKey,
                              @PathVariable String identifier, Principal principal, RedirectAttributes redirectAttributes) {
        User currentUser = userRepository.findByEmail(principal.getName());

        if(currentUser.getOrganization() == null)
            return "organization/select-organization";

        Issue issue = findIssueOrNotFound(currentUser, identifier, issueKey);
        Comment updatedComment = findOwnCommentOrNotFound(id, issue, currentUser);

        if(bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("commentError", COMMENT_ERROR);
            return "redirect:/issues/{identifier}/view/" + issueKey;
        }

        // sets Comment isEdited to true if comment has not yet been edited
        if(!isEdited) { updatedComment.setIsEdited(true); }
        updatedComment.setMessage(form.message());
        commentRepository.save(updatedComment);

        return "redirect:/issues/{identifier}/view/" + issueKey;
    }

    @PostMapping("/{commentId}/delete")
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
        if(currentProject == null || !OrganizationScope.isMember(currentUser, currentProject))
            throw OrganizationScope.notFound();

        Issue issue = issueRepository.findByIssueKeyAndProject(issueKey, currentProject);
        if(issue == null)
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
