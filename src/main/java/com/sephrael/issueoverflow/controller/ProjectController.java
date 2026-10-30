package com.sephrael.issueoverflow.controller;

import com.sephrael.issueoverflow.controller.form.ProjectForm;
import com.sephrael.issueoverflow.entity.*;
import com.sephrael.issueoverflow.repository.IssueKeySequenceRepository;
import com.sephrael.issueoverflow.repository.IssueRepository;
import com.sephrael.issueoverflow.repository.ProjectRepository;
import com.sephrael.issueoverflow.repository.UserRepository;
import com.sephrael.issueoverflow.service.AWSFileService;
import com.sephrael.issueoverflow.service.IssueService;
import com.sephrael.issueoverflow.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/projects")
public class ProjectController {

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private IssueRepository issueRepository;
    @Autowired
    private IssueService issueService;
    @Autowired
    private IssueKeySequenceRepository issueKeySequenceRepository;
    @Autowired
    private AWSFileService awsFileService;

    @RequestMapping("/all")
    public String viewProjects(Model model, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());

        if(currentUser.getOrganization() == null)
            return "organization/select-organization";

        model.addAttribute("newProject", new Project());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentUserProjects", currentUser.getProjects());

        return "projects/projects-list";
    }

    @RequestMapping("/{identifier}")
    public String projectHome(@PathVariable("identifier") String identifier, Model model, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());
        Organization currentOrganization = currentUser.getOrganization();

        if(currentOrganization == null)
            return "organization/select-organization";

        Project currentProject = projectRepository.findByIdentifierAndOrganization(identifier, currentOrganization);

        List<Issue> sortedIssues = issueService.getIssuesSortedByRecentActivity(
                issueRepository.findByProject(currentProject),
                issueRepository.findByProjectOrderByDateCreatedDesc(currentProject),
                issueRepository.findByProjectOrderByDateUpdatedDesc(currentProject)
        );

        model.addAttribute("issues", sortedIssues);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("newProject", new Project());
        model.addAttribute("issueService", issueService);
        model.addAttribute("seconds", ChronoUnit.SECONDS);
        model.addAttribute("currentProject", currentProject);
        model.addAttribute("currentOrganizationMembers", userRepository.findByOrganizationOrderByRoleId(currentOrganization));
        model.addAttribute("issueRepository", issueRepository);
        model.addAttribute("currentUserProjects", currentUser.getProjects());
        model.addAttribute("numberOfOpenIssues", issueService.getNumberOfIssuesByProjectAndStatus("Open", identifier, currentOrganization));
        model.addAttribute("numberOfClosedIssues", issueService.getNumberOfIssuesByProjectAndStatus("Closed", identifier, currentOrganization));
        model.addAttribute("numberOfResolvedIssues", issueService.getNumberOfIssuesByProjectAndStatus("Resolved", identifier, currentOrganization));
        model.addAttribute("numberOfInProgressIssues", issueService.getNumberOfIssuesByProjectAndStatus("In-Progress", identifier, currentOrganization));

        return "projects/project-home";
    }

    @PostMapping("/new")
    public String createProject(ProjectForm form, Principal principal, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = userRepository.findByEmail(principal.getName());
        Organization currentUserOrganization = currentUser.getOrganization();

        if(currentUserOrganization == null)
            return "organization/select-organization";

        model.addAttribute("project", new Project());

        Project newProject = new Project();
        newProject.setName(form.name());
        newProject.setIdentifier(form.identifier());
        newProject.setColor(form.color());

        // verifies that the Project requested for Creation has unique fields and redirects to '/projects/all' with an
        // alert message based on the result
        return projectService.isProjectIdentifierUniqueWithinOrganization(newProject, currentUser,
                true, redirectAttributes);
    }

    @PostMapping("/save")
    public String editProject(@RequestParam(value = "id") Long id, ProjectForm form, Principal principal,
                              RedirectAttributes redirectAttributes) {
        User currentUser = userRepository.findByEmail(principal.getName());
        Organization currentOrganization = currentUser.getOrganization();

        if(currentOrganization == null)
            return "organization/select-organization";

        // another organization's project looks the same as one that doesn't exist
        Project projectBeforeUpdate = projectRepository.findByIdAndOrganization(id, currentOrganization);

        if(projectBeforeUpdate == null)
            throw OrganizationScope.notFound();

        // the form sets only the name, identifier and color; the project stays in the caller's organization
        Project requestedProjectUpdate = new Project();
        requestedProjectUpdate.setId(projectBeforeUpdate.getId());
        requestedProjectUpdate.setOrganization(currentOrganization);
        requestedProjectUpdate.setName(form.name());
        requestedProjectUpdate.setIdentifier(form.identifier());
        requestedProjectUpdate.setColor(form.color());

        // if a Project's Identifier is requested to be changed, create a new Issue Key Sequence and delete the previous one
        if(!Objects.equals(projectBeforeUpdate.getIdentifier(), requestedProjectUpdate.getIdentifier())) {
            IssueKeySequence previousIssueKeySequence = issueKeySequenceRepository.findByProjectIdentifierAndProjectId(
                    projectBeforeUpdate.getIdentifier(), projectBeforeUpdate.getId());

            IssueKeySequence newIssueKeySequence = new IssueKeySequence();
            newIssueKeySequence.setProjectId(requestedProjectUpdate.getId());
            newIssueKeySequence.setProjectIdentifier(requestedProjectUpdate.getIdentifier());
            newIssueKeySequence.setIssueKeyCounter(previousIssueKeySequence.getIssueKeyCounter());
            issueKeySequenceRepository.save(newIssueKeySequence);

            issueKeySequenceRepository.delete(previousIssueKeySequence);
        }

        // fixes bug that when updating a Project removed all the Users in an updated Project except for the User that
        // performed the update
        // had to use 'projectBeforeUpdate' because I needed to access a Project's Users before the update was performed
        for (User user : projectBeforeUpdate.getUsers()) {
            requestedProjectUpdate.addUser(user);
        }

        // verifies that the Project requested for an Update has unique fields and redirects to '/projects/all' with an
        // alert message based on the result
        return projectService.isProjectIdentifierUniqueWithinOrganization(requestedProjectUpdate, currentUser,
                false, redirectAttributes);
    }

    @RequestMapping("/delete/{id}")
    public String deleteProject(@PathVariable(name = "id") Long id, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());

        if(currentUser.getOrganization() == null)
            return "organization/select-organization";

        // checks if the Current User is involved with the Requested Project
        if(!currentUser.getProjects().contains(projectRepository.findProjectById(id))) {
            return "error/404";
        }

        // deletes all the files associated with this project, from AWS S3 bucket
        awsFileService.deleteAllFilesByProject(projectRepository.findProjectById(id));

        projectRepository.deleteById(id);

        return "redirect:/projects/all";
    }

//    @GetMapping(path = "/json")
//    public @ResponseBody
//    List<Project> getAllProjects() {
//        return projectRepository.findAll();
//    }
}
