package com.sephrael.issueoverflow.controller;

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
    public String addComment(@PathVariable(value = "issueKey") String issueKey, Comment comment, Principal principal,
                             @PathVariable String identifier, HttpServletRequest request) throws MessagingException, UnsupportedEncodingException {
        User currentUser = userRepository.findByEmail(principal.getName());
        Project currentProject = projectRepository.findByIdentifierAndOrganization(identifier, currentUser.getOrganization());

        if(currentUser.getOrganization() == null)
            return "organization/select-organization";

        // checks if the Current User is involved with the requested Project
        if(!currentUser.getProjects().contains(currentProject))
            return "error/404";

        Issue issue = issueRepository.findByIssueKeyAndOrganization(issueKey, currentUser.getOrganization());

        currentUser.addToComment(comment);
        issue.addToComment(comment);

        comment.setIsEdited(false);
        commentRepository.save(comment);

        userService.sendEmailNotification(issue, comment, request, false, true);

        return "redirect:/issues/{identifier}/view/" + issueKey;
    }

    @PostMapping("/save")
    public String editComment(@RequestParam("id") Long id, @RequestParam("user") User user, @RequestParam("issue") Issue issue,
                              @RequestParam("isEdited") boolean isEdited, @RequestParam("message") String message,
                              @PathVariable(value = "issueKey") String issueKey,
                              @PathVariable String identifier, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());
        Project currentProject = projectRepository.findByIdentifierAndOrganization(identifier, currentUser.getOrganization());

        if(currentUser.getOrganization() == null)
            return "organization/select-organization";

        // checks if the Current User is involved with the requested Project
        if(!currentUser.getProjects().contains(currentProject))
            return "error/404";

        Comment updatedComment = commentRepository.findCommentById(id);

        updatedComment.setUser(user);
        updatedComment.setIssue(issue);

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
        Project currentProject = projectRepository.findByIdentifierAndOrganization(identifier, currentUser.getOrganization());

        if(currentUser.getOrganization() == null)
            return "organization/select-organization";

        // checks if the Current User is involved with the requested Project
        if(!currentUser.getProjects().contains(currentProject))
            return "error/404";


        commentRepository.deleteById(commentId);

        return "redirect:/issues/{identifier}/view/" + issueKey;
    }

//    @GetMapping(path = "/json")
//    public @ResponseBody Iterable<Comment> getAllCommentsByIssueId(@PathVariable(value = "issueKey") String issueKey,
//                                                                   @PathVariable(name = "identifier") String identifier, Principal principal) {
//        return issueRepository.findByIssueKeyAndOrganization(issueKey,
//                userRepository.findByEmail(principal.getName()).getOrganization()).getComments();
//    }
}
