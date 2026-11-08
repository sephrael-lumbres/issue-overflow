package com.sephrael.issueoverflow.controller;

import com.sephrael.issueoverflow.controller.form.IssueForm;
import com.sephrael.issueoverflow.controller.form.IssueStatusForm;
import com.sephrael.issueoverflow.entity.*;
import com.sephrael.issueoverflow.repository.*;
import com.sephrael.issueoverflow.service.AWSFileService;
import com.sephrael.issueoverflow.service.IssueService;
import com.sephrael.issueoverflow.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/issues")
public class IssueController {

    private static final String ISSUE_FORM_ERROR = "Please enter a title, and check that the title and description aren't too long.";

    @Autowired
    private IssueRepository issueRepository;
    @Autowired
    private IssueService issueService;
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

        Project currentProject = findProjectOrNotFound(identifier, currentUser);

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

        User currentUser = userRepository.findByEmail(principal.getName());
        Project currentProject = projectRepository.findByIdentifierAndOrganization(identifier, currentUser.getOrganization());
        String URL = issueService.getFilteredIssues(type, status, priority, createdBy, assignedTo, identifier, model, principal);

        // if Current User does not belong to any Organization, redirect them to the 'Select Organization' page
        if(URL != null) return URL;

        if(currentProject == null || !OrganizationScope.isMember(currentUser, currentProject))
            throw OrganizationScope.notFound();

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
        model.addAttribute("currentProject", findProjectOrNotFound(identifier, currentUser));
        model.addAttribute("currentUserProjects", currentUser.getProjects());

        // OLD CODE: this was previously used to CONNECT an 'Issue' to the 'User' that created the 'Issue'
        //model.addAttribute("users", userService.listAll());

        return "issues/create-issue";
    }

    @PostMapping(value = "/new")
    public String saveNewIssue(@Valid IssueForm form, BindingResult bindingResult, Principal principal,
                               @RequestParam(value = "files", required = false) MultipartFile[] files,
                               RedirectAttributes redirectAttributes) throws MessagingException, UnsupportedEncodingException {
        User currentUser = userRepository.findByEmail(principal.getName());
        Organization currentOrganization = currentUser.getOrganization();

        if(currentOrganization == null)
            return "organization/select-organization";

        if(bindingResult.hasErrors()) {
            Project project = projectOrNotFound(form.project(), currentUser);
            redirectAttributes.addFlashAttribute("issueFormError", ISSUE_FORM_ERROR);
            return "redirect:/issues/" + project.getIdentifier() + "/new";
        }

        Issue issue = new Issue();
        copyFormFields(form, issue);
        issue.setProject(projectOrNotFound(form.project(), currentUser));
        issue.setAssignedTo(assigneeInOrganization(form.assignedTo(), currentOrganization));

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

        userService.sendEmailNotification(issue, null, true, false);

        return ("redirect:/issues/" + issue.getProject().getIdentifier() + "/view/" + issue.getIssueKey());
    }

    @PostMapping(value = "/update")
    public String updateIssue(@RequestParam("id") Long id, @Valid IssueForm form, BindingResult bindingResult, Principal principal,
                              @RequestParam(value = "files", required = false) MultipartFile[] files,
                              @RequestParam(value = "isAttachFileForm", required = false) boolean isAttachFileForm,
                              RedirectAttributes redirectAttributes) throws MessagingException, UnsupportedEncodingException {
        User currentUser = userRepository.findByEmail(principal.getName());
        Organization currentOrganization = currentUser.getOrganization();

        if(currentOrganization == null)
            return "organization/select-organization";

        // another organization's issue, and one in a project the caller isn't a member of, look the same as one that doesn't exist
        Issue previousIssue = issueRepository.findByIdAndOrganization(id, currentOrganization);

        if(previousIssue == null || !OrganizationScope.isMember(currentUser, previousIssue.getProject()))
            throw OrganizationScope.notFound();

        // the attach-file form sends only the id and the files, so the other fields are checked only for the edit form
        if(!isAttachFileForm && bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("issueFormError", ISSUE_FORM_ERROR);
            return "redirect:/issues/" + previousIssue.getProject().getIdentifier() + "/edit/" + previousIssue.getIssueKey();
        }

        if(!isAttachFileForm) {
            Project nextProject = projectOrNotFound(form.project(), currentUser);
            User nextAssignee = assigneeInOrganization(form.assignedTo(), currentOrganization);

            previousIssue.setUpdatedBy(currentUser);
            copyFormFields(form, previousIssue);
            issueService.setIssueKey(previousIssue, nextProject);
            previousIssue.setAssignedTo(nextAssignee);
            previousIssue.setProject(nextProject);

            issueService.save(previousIssue);
        }

        // if 'Attach File(s)' Field is NOT null AND NOT empty, they are uploaded to the DB and are CONNECTED to the requested 'Issue'
        if(files != null && !files[0].isEmpty()) {
            for (MultipartFile multipartFile : files) {
                awsFileService.uploadFile(multipartFile, currentUser, previousIssue, false);
            }
        }

        userService.sendEmailNotification(previousIssue, null, false, false);

        return ("redirect:/issues/" + previousIssue.getProject().getIdentifier() + "/view/" + previousIssue.getIssueKey());
    }

    // The Mark as Resolved and Mark as Closed buttons. Only the status and who changed it are written; the edit form
    // (/issues/update) sends every field, and posting it from these buttons cleared the estimate and the due date.
    @PostMapping("/{identifier}/view/{issueKey}/status")
    public String changeIssueStatus(@PathVariable("identifier") String identifier, @PathVariable("issueKey") String issueKey,
                                    @Valid IssueStatusForm form, BindingResult bindingResult, Principal principal,
                                    Authentication authentication) throws MessagingException, UnsupportedEncodingException {
        User currentUser = userRepository.findByEmail(principal.getName());
        if(currentUser.getOrganization() == null)
            return "organization/select-organization";

        Project currentProject = findProjectOrNotFound(identifier, currentUser);
        Issue issue = findIssueOrNotFound(issueKey, currentProject);

        if(bindingResult.hasErrors())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

        // the page offers Mark as Closed to Project Managers and Admins only
        if("Closed".equals(form.status()) && authentication.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("Project Manager") || a.getAuthority().equals("Admin")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        issue.setStatus(form.status());
        issue.setUpdatedBy(currentUser);
        issueService.save(issue);

        userService.sendEmailNotification(issue, null, false, false);

        return "redirect:/issues/" + identifier + "/view/" + issueKey;
    }

    @RequestMapping("/{identifier}/view/{issueKey}")
    public String showViewIssuePage(@PathVariable("issueKey") String issueKey, Model model,
                                    @PathVariable(name = "identifier") String identifier, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());
        Organization currentOrganization = currentUser.getOrganization();

        if(currentOrganization == null)
            return "organization/select-organization";

        Project currentProject = findProjectOrNotFound(identifier, currentUser);
        Issue issue = findIssueOrNotFound(issueKey, currentProject);
        model.addAttribute("newComment", new Comment());
        model.addAttribute("comments", issue.getComments());
        model.addAttribute("issue", issue);
        model.addAttribute("attachments", issue.getAWSFiles());
        model.addAttribute("issueChangeHistoryList", issueService.getIssueChangeHistoryList(issue.getId()));
        model.addAttribute("findRevisions", issueRepository.findRevisions(issue.getId()));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentProject", currentProject);
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

        Project currentProject = findProjectOrNotFound(identifier, currentUser);
        Issue currentIssue = findIssueOrNotFound(issueKey, currentProject);

        // if Current User did NOT Open the requested Issue AND is NOT a Project Manager AND is NOT an Admin, display 403 error
        if(!Objects.equals(currentIssue.getUser().getId(), currentUser.getId()) &&
                authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("Project Manager")) &&
                authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("Admin")))
            return "error/403";

        model.addAttribute("issue", currentIssue);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentProject", currentProject);
        model.addAttribute("currentUserProjects", currentUser.getProjects());

        return "issues/edit-issue";
    }

    @PostMapping("/{identifier}/delete/{issueKey}/{isOrganizationList}")
    public String deleteIssue(@PathVariable(name = "issueKey") String issueKey,
                              @PathVariable(name = "identifier") String identifier, Principal principal,
                              @PathVariable("isOrganizationList") boolean isOrganizationList, Authentication authentication) {
        User currentUser = userRepository.findByEmail(principal.getName());
        Organization currentOrganization = currentUser.getOrganization();

        if(currentUser.getOrganization() == null)
            return "organization/select-organization";

        Project currentProject = findProjectOrNotFound(identifier, currentUser);
        Issue currentIssue = findIssueOrNotFound(issueKey, currentProject);

        // if Current User did NOT Open the requested Issue AND is NOT a Project Manager AND is NOT an Admin, display 403 error
        if(!Objects.equals(currentIssue.getUser().getId(), currentUser.getId()) &&
                authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("Project Manager")) &&
                authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("Admin")))
            return "error/403";

        // deletes all the files associated with this issue, from the AWS S3 bucket
        awsFileService.deleteAllFilesByIssue(currentIssue);
        issueService.delete(currentIssue.getId());

        if(isOrganizationList)
            return "redirect:/issues/all";
        else
            return "redirect:/issues/" + identifier;
    }

    // The project named in the URL, within the caller's organization, which the caller must be a member of.
    private Project findProjectOrNotFound(String identifier, User currentUser) {
        Project project = projectRepository.findByIdentifierAndOrganization(identifier, currentUser.getOrganization());
        if(project == null || !OrganizationScope.isMember(currentUser, project))
            throw OrganizationScope.notFound();

        return project;
    }

    // The issue named in the URL, within the URL's project (keys are unique per project, not per organization).
    private Issue findIssueOrNotFound(String issueKey, Project project) {
        Issue issue = issueRepository.findByIssueKeyAndProject(issueKey, project);
        if(issue == null)
            throw OrganizationScope.notFound();

        return issue;
    }

    private static void copyFormFields(IssueForm form, Issue issue) {
        issue.setTitle(form.title());
        issue.setDescription(form.description());
        issue.setType(form.type());
        issue.setPriority(form.priority());
        issue.setStatus(form.status());
        issue.setEstimatedHours(form.estimatedHours());
        issue.setDueDate(form.dueDate());
    }

    // The project chosen in a form, which must belong to the caller's organization and have the caller as a member.
    private Project projectOrNotFound(Long projectId, User currentUser) {
        Project project = projectId == null ? null : projectRepository.findByIdAndOrganization(projectId, currentUser.getOrganization());
        if(project == null || !OrganizationScope.isMember(currentUser, project))
            throw OrganizationScope.notFound();

        return project;
    }

    // The assignee chosen in a form: none (the list sends " "), or a user in the caller's organization.
    private User assigneeInOrganization(String assignedTo, Organization organization) {
        if(!StringUtils.hasText(assignedTo))
            return null;

        User assignee;
        try {
            assignee = userRepository.findByIdAndOrganization(Long.valueOf(assignedTo.trim()), organization);
        } catch(NumberFormatException exception) {
            throw OrganizationScope.notFound();
        }

        if(assignee == null)
            throw OrganizationScope.notFound();

        return assignee;
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