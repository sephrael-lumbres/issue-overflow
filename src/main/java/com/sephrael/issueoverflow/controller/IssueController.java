package com.sephrael.issueoverflow.controller;

import com.sephrael.issueoverflow.entity.*;
import com.sephrael.issueoverflow.repository.*;
import com.sephrael.issueoverflow.service.AWSFileService;
import com.sephrael.issueoverflow.service.IssueService;
import com.sephrael.issueoverflow.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/issues")
public class IssueController {

    @Autowired
    private IssueRepository issueRepository;
    @Autowired
    private IssueService issueService;
    @Autowired
    private IssueKeySequenceRepository issueKeySequenceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private AWSFileService awsFileService;

    @GetMapping("/all")
    public String showIssuesByOrganization(Principal principal, Model model) {
        User currentUser = userRepository.findByEmail(principal.getName());

        if(currentUser.getOrganization() == null)
            return "organization/select-organization";

        List<Issue> issuesByOrganization = issueRepository.findByOrganization(currentUser.getOrganization());

        model.addAttribute("newProject", new Project());
        model.addAttribute("issues", issuesByOrganization);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentUserProjects", currentUser.getProjects());

        return "issues/issues-by-organization";
    }
    
    @GetMapping("/all/results")
    public String showFilteredIssuesByOrganizationPage(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "createdBy", required = false) String createdBy,
            @RequestParam(value = "assignedTo", required = false) String assignedTo,
            @RequestParam(value = "identifier", required = false) String identifier, Model model, Principal principal) {

        String URL = issueService.getFilteredIssues(type, status, priority, createdBy, assignedTo, identifier, model, principal);

        // if Current User does not belong to any Organization, redirect them to the 'Select Organization' page
        if(URL != null) return URL;

        model.addAttribute("newProject", new Project());

        return "issues/issues-by-organization";
    }

    @GetMapping("/{identifier}")
    public String showIssuesByProject(@PathVariable("identifier") String identifier, Model model, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());
        Organization currentOrganization = currentUser.getOrganization();

        if(currentOrganization == null)
            return "organization/select-organization";

        Project currentProject = projectRepository.findByIdentifierAndOrganization(identifier, currentOrganization);

        model.addAttribute("issues", currentProject.getIssues());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentProject", currentProject);
        model.addAttribute("currentUserProjects", currentUser.getProjects());

        return "issues/issues-by-project";
    }

    @GetMapping("/{identifier}/results")
    public String showFilteredIssuesByProjectPage(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "createdBy", required = false) String createdBy,
            @RequestParam(value = "assignedTo", required = false) String assignedTo,
            @PathVariable("identifier") String identifier, Model model, Principal principal) {

        Project currentProject = projectRepository.findByIdentifierAndOrganization(identifier,
                userRepository.findByEmail(principal.getName()).getOrganization());
        String URL = issueService.getFilteredIssues(type, status, priority, createdBy, assignedTo, identifier, model, principal);

        // if Current User does not belong to any Organization, redirect them to the 'Select Organization' page
        if(URL != null) return URL;

        model.addAttribute("currentProject", currentProject);

        return "issues/issues-by-project";
    }

    @RequestMapping("/{identifier}/new")
    public String showNewIssuePage(@PathVariable(name = "identifier") String identifier, Model model, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());
        Organization currentOrganization = currentUser.getOrganization();

        if(currentOrganization == null)
            return "organization/select-organization";

        Issue issue = new Issue();
        model.addAttribute("issue", issue);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentProject", projectRepository.findByIdentifierAndOrganization(identifier, currentOrganization));
        model.addAttribute("currentUserProjects", currentUser.getProjects());

        // OLD CODE: this was previously used to CONNECT an 'Issue' to the 'User' that created the 'Issue'
        //model.addAttribute("users", userService.listAll());

        return "issues/create-issue";
    }

    @PostMapping(value = "/new")
    public String saveNewIssue(@ModelAttribute("issue") Issue issue, Principal principal, HttpServletRequest request,
                               @RequestParam(value = "files", required = false)MultipartFile[] files) throws MessagingException, UnsupportedEncodingException {
        User currentUser = userRepository.findByEmail(principal.getName());

        if(currentUser.getOrganization() == null)
            return "organization/select-organization";

        // sets the Issue Key
        issueService.setIssueKey(issue, issue.getProject());

        // connects the newly created Issue to the current User that created the Issue
        currentUser.addToIssue(issue);

        // connects the issue to the current user's Organization
        currentUser.getOrganization().addToIssue(issue);

        issueService.save(issue);

        // if 'Attach File(s)' Field is NOT empty, they are uploaded to the DB and are CONNECTED to the requested 'Issue'
        if(!files[0].isEmpty()) {
            for (MultipartFile multipartFile : files) {
                awsFileService.uploadFile(multipartFile, currentUser, issue, false);
            }
        }

        userService.sendEmailNotification(issue, null, request, true, false);

        return ("redirect:/issues/" + issue.getProject().getIdentifier() + "/view/" + issue.getIssueKey());
    }

    @PostMapping(value = "/update")
    public String updateIssue(@ModelAttribute("issue") Issue nextIssue, Principal principal, HttpServletRequest request,
                              @RequestParam(value = "files", required = false)MultipartFile[] files,
                              @RequestParam(value = "isAttachFileForm", required = false) boolean isAttachFileForm) throws MessagingException, UnsupportedEncodingException {
        User currentUser = userRepository.findByEmail(principal.getName());

        if(currentUser.getOrganization() == null)
            return "organization/select-organization";

        Issue previousIssue = issueRepository.findIssueById(nextIssue.getId());

        if(!isAttachFileForm) {
            previousIssue.setUpdatedBy(currentUser);
            previousIssue.setTitle(nextIssue.getTitle());
            previousIssue.setDescription(nextIssue.getDescription());
            previousIssue.setType(nextIssue.getType());
            previousIssue.setPriority(nextIssue.getPriority());
            previousIssue.setStatus(nextIssue.getStatus());
            previousIssue.setEstimatedHours(nextIssue.getEstimatedHours());
            previousIssue.setDueDate(nextIssue.getDueDate());
            issueService.setIssueKey(previousIssue, nextIssue.getProject());
            previousIssue.setAssignedTo(nextIssue.getAssignedTo());
            previousIssue.setProject(nextIssue.getProject());

            issueService.save(previousIssue);
        }

        // if 'Attach File(s)' Field is NOT null AND NOT empty, they are uploaded to the DB and are CONNECTED to the requested 'Issue'
        if(files != null && !files[0].isEmpty()) {
            for (MultipartFile multipartFile : files) {
                awsFileService.uploadFile(multipartFile, currentUser, previousIssue, false);
            }
        }

        userService.sendEmailNotification(previousIssue, null, request, false, false);

        return ("redirect:/issues/" + previousIssue.getProject().getIdentifier() + "/view/" + previousIssue.getIssueKey());
    }

    @RequestMapping("/{identifier}/view/{issueKey}")
    public String showViewIssuePage(@PathVariable("issueKey") String issueKey, Model model,
                                    @PathVariable(name = "identifier") String identifier, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());
        Organization currentOrganization = currentUser.getOrganization();

        if(currentOrganization == null)
            return "organization/select-organization";

        Issue issue = issueRepository.findByIssueKeyAndOrganization(issueKey, currentOrganization);
        model.addAttribute("newComment", new Comment());
        model.addAttribute("comments", issue.getComments());
        model.addAttribute("issue", issue);
        model.addAttribute("attachments", issue.getAWSFiles());
        model.addAttribute("issueChangeHistoryList", issueService.getIssueChangeHistoryList(issue.getId()));
        model.addAttribute("findRevisions", issueRepository.findRevisions(issue.getId()));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentProject", projectRepository.findByIdentifierAndOrganization(identifier, currentOrganization));
        model.addAttribute("currentUserProjects", currentUser.getProjects());

        return "issues/issue-details";
    }

    @GetMapping("/{identifier}/edit/{issueKey}")
    public String showEditIssuePage(@PathVariable("issueKey") String issueKey, Authentication authentication,
                                    @PathVariable(name = "identifier") String identifier, Model model, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());
        Organization currentOrganization = currentUser.getOrganization();

        if(currentOrganization == null)
            return "organization/select-organization";

        Issue currentIssue = issueRepository.findByIssueKeyAndOrganization(issueKey, currentOrganization);

        // if Current User did NOT Open the requested Issue AND is NOT a Project Manager AND is NOT an Admin, display 403 error
        if(!Objects.equals(currentIssue.getUser().getId(), currentUser.getId()) &&
                authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("Project Manager")) &&
                authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("Admin")))
            return "error/403";

        model.addAttribute("issue", currentIssue);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentProject", projectRepository.findByIdentifierAndOrganization(identifier, currentOrganization));
        model.addAttribute("currentUserProjects", currentUser.getProjects());

        return "issues/edit-issue";
    }

    @RequestMapping("/{identifier}/delete/{issueKey}/{isOrganizationList}")
    public String deleteIssue(@PathVariable(name = "issueKey") String issueKey,
                              @PathVariable(name = "identifier") String identifier, Principal principal,
                              @PathVariable("isOrganizationList") boolean isOrganizationList, Authentication authentication) {
        User currentUser = userRepository.findByEmail(principal.getName());
        Organization currentOrganization = currentUser.getOrganization();

        if(currentUser.getOrganization() == null)
            return "organization/select-organization";

        Issue currentIssue = issueRepository.findByIssueKeyAndOrganization(issueKey, currentOrganization);

        // if Current User did NOT Open the requested Issue AND is NOT a Project Manager AND is NOT an Admin, display 403 error
        if(!Objects.equals(currentIssue.getUser().getId(), currentUser.getId()) &&
                authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("Project Manager")) &&
                authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("Admin")))
            return "error/403";

        Project currentProject = projectRepository.findByIdentifierAndOrganization(identifier, currentOrganization);

        // deletes all the files associated with this issue, from the AWS S3 bucket
        awsFileService.deleteAllFilesByIssue(currentIssue);

        issueService.delete(currentIssue.getId());

        if(currentProject.getIssues().isEmpty()) {
            issueKeySequenceRepository.delete(issueKeySequenceRepository.findByProjectIdentifierAndProjectId(identifier, currentProject.getId()));
        }

        if(isOrganizationList)
            return "redirect:/issues/all";
        else
            return "redirect:/issues/" + identifier;
    }

//    @GetMapping(path = "/json")
//    public @ResponseBody Iterable<Issue> getAllIssues() {
//        return issueService.listAll();
//    }
//
//    @GetMapping(path = "/{identifier}/json")
//    public @ResponseBody Iterable<Issue> getAllIssuesByProject(@PathVariable("identifier") String identifier, Principal principal) {
//        return issueService.findProjectByIdentifierAndOrganization(identifier, userRepository.findByEmail(principal.getName()).getOrganization());
//    }
//
//    @GetMapping(path = "/comments/json")
//    public @ResponseBody Iterable<Comment> getAllComments() {
//        return commentRepository.findAll();
//    }
}