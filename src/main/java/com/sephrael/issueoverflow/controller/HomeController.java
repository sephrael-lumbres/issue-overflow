package com.sephrael.issueoverflow.controller;

import com.sephrael.issueoverflow.entity.Issue;
import com.sephrael.issueoverflow.entity.Organization;
import com.sephrael.issueoverflow.entity.Project;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.repository.IssueRepository;
import com.sephrael.issueoverflow.repository.UserRepository;
import com.sephrael.issueoverflow.service.IssueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.security.Principal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Controller
public class HomeController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private IssueRepository issueRepository;
    @Autowired
    private IssueService issueService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String redirectToDashboard() {
        return "redirect:/dashboard";
    }

    @RequestMapping(value = "/dashboard", method = RequestMethod.GET)
    public String dashboard(Model model, Principal principal) {

        // optional: add comments activity to dashboard
//        model.addAttribute("comments", commentRepository.findByUserOrganizationOrderByDateCreatedDesc(userRepository.findByEmail(principal.getName()).getOrganization()));

        User currentUser = userRepository.findByEmail(principal.getName());
        Organization currentOrganization = userRepository.findByEmail(principal.getName()).getOrganization();

        if(currentOrganization == null)
            return "organization/select-organization";

        List<Issue> sortedIssues = issueService.getIssuesSortedByRecentActivity(
                issueRepository.findByOrganization(currentOrganization),
                issueRepository.findByOrganizationOrderByDateCreatedDesc(currentOrganization),
                issueRepository.findByOrganizationOrderByDateUpdatedDesc(currentOrganization)
        );

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("createdByCurrentUser", issueRepository.findByUser(currentUser));
        model.addAttribute("assignedToCurrentUser", issueRepository.findByAssignedTo(currentUser));
        model.addAttribute("newProject", new Project());
        model.addAttribute("issueRepository", issueRepository);
        model.addAttribute("issueService", issueService);
        model.addAttribute("issues", sortedIssues);
        model.addAttribute("seconds", ChronoUnit.SECONDS);
        model.addAttribute("currentUserProjects", currentUser.getProjects());
        model.addAttribute("usersByOrganization", userRepository.findByOrganizationOrderByRoleId(currentOrganization));
        model.addAttribute("numberOfOpenIssues", issueService.getNumberOfIssuesByOrganizationAndStatus("Open", currentUser));
        model.addAttribute("numberOfClosedIssues", issueService.getNumberOfIssuesByOrganizationAndStatus("Closed", currentUser));
        model.addAttribute("numberOfResolvedIssues", issueService.getNumberOfIssuesByOrganizationAndStatus("Resolved", currentUser));
        model.addAttribute("numberOfInProgressIssues", issueService.getNumberOfIssuesByOrganizationAndStatus("In-Progress", currentUser));

        return "dashboard";
    }
}
