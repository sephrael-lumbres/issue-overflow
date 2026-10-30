package com.sephrael.issueoverflow.controller;

import com.sephrael.issueoverflow.controller.form.OrganizationForm;
import com.sephrael.issueoverflow.entity.Organization;
import com.sephrael.issueoverflow.entity.Project;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.repository.OrganizationRepository;
import com.sephrael.issueoverflow.repository.RoleRepository;
import com.sephrael.issueoverflow.repository.UserRepository;
import com.sephrael.issueoverflow.service.TokenGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Objects;

@Controller
@RequestMapping("/organization")
public class OrganizationController {

    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;

    @RequestMapping("")
    public String viewOrganizationDetails(Model model, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());
        Organization currentOrganization = currentUser.getOrganization();

        if(currentOrganization == null) {
            return "organization/select-organization";
        }

        model.addAttribute("newProject", new Project());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentUserProjects", currentUser.getProjects());

        // gets the Users of an Organization by each Role
        model.addAttribute("projectManagers", userRepository.findByOrganizationAndRole(currentOrganization, roleRepository.findByName("Project Manager")));
        model.addAttribute("admins", userRepository.findByOrganizationAndRole(currentOrganization, roleRepository.findByName("Admin")));
        model.addAttribute("developers", userRepository.findByOrganizationAndRole(currentOrganization, roleRepository.findByName("Developer")));
        model.addAttribute("guests", userRepository.findByOrganizationAndRole(currentOrganization, roleRepository.findByName("Guest")));

        return "organization/organization-details";
    }

    @RequestMapping("/select")
    public String showCreateOrJoinOrganizationPage(Principal principal) {

        if(userRepository.findByEmail(principal.getName()).getOrganization() != null)
            return "redirect:/organization";
        else
            return "organization/select-organization";
    }

    @RequestMapping("/new")
    public String showCreateOrganizationPage(Model model, Principal principal) {
        if(userRepository.findByEmail(principal.getName()).getOrganization() != null)
            return "redirect:/organization";

        model.addAttribute("organization", new Organization());

        return "organization/create-organization";
    }

    @PostMapping("/save")
    public String createOrganization(OrganizationForm form, Principal principal,
                                     HttpServletRequest request, HttpServletResponse response) {
        User currentUser = userRepository.findByEmail(principal.getName());

        // a member of an organization must leave it before creating another
        if(currentUser.getOrganization() != null)
            return "redirect:/organization";

        Organization organization = new Organization();
        organization.setName(form.name());
        organization.setAccessKey(TokenGenerator.newAccessKey());

        // this connects the current user to the newly created Organization
        organization.addToUser(currentUser);
        organizationRepository.save(organization);

        roleRepository.findByName("Project Manager").addToUser(currentUser);
        userRepository.save(currentUser);

        // logs out the Current User to allow the Authorization of 'Project Manager'
        new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());

        return "redirect:/dashboard";
    }

    @PostMapping("/update")
    public String updateOrganization(@RequestParam(value = "id") Long id, RedirectAttributes redirectAttributes,
                                     @RequestParam(value = "organizationName") String organizationName, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());
        Organization currentOrganization = currentUser.getOrganization();

        if(currentOrganization == null)
            return "organization/select-organization";

        // only the caller's own organization can be renamed; any other id looks unknown
        if(!Objects.equals(id, currentOrganization.getId()))
            throw OrganizationScope.notFound();

        try {
            currentOrganization.setName(organizationName);
            organizationRepository.save(currentOrganization);

            redirectAttributes.addFlashAttribute("updateOrganizationSuccess", "Organization has been successfully updated!");
        } catch(Exception exception) {
            redirectAttributes.addFlashAttribute("updateOrganizationError", "An error has occurred while attempting to save Organization changes");;
        }

        return "redirect:/organization";
    }

    @PostMapping("/joining")
    public String joinOrganization(@RequestParam(value = "accessKey") String accessKey, Principal principal,
                                   HttpServletRequest request, HttpServletResponse response,
                                   RedirectAttributes redirectAttributes) {
        User currentUser = userRepository.findByEmail(principal.getName());

        // a member of an organization must leave it before joining another; otherwise they would keep their role
        if(currentUser.getOrganization() != null)
            return "redirect:/organization";

        Organization organization = organizationRepository.findByAccessKey(accessKey);

        if(organization == null) {
            redirectAttributes.addFlashAttribute("joinOrganizationError", "No organization matches that access code.");
            return "redirect:/organization/join";
        }

        // the first member of an Organization becomes its Project Manager; everyone after joins as a Guest
        boolean isFirstMember = organization.getUsers().isEmpty();
        roleRepository.findByName(isFirstMember ? "Project Manager" : "Guest").addToUser(currentUser);
        organization.addToUser(currentUser);
        userRepository.save(currentUser);

        // logs out the Current User to allow the Authorization of 'Project Manager'
        if(isFirstMember)
            new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());

        return "redirect:/dashboard";
    }

    @RequestMapping("/join")
    public String joinOrganization(Principal principal) {
        if(userRepository.findByEmail(principal.getName()).getOrganization() != null)
            return "redirect:/organization";

        return "organization/join-organization";
    }

//    @GetMapping(path = "/json")
//    public @ResponseBody
//    Iterable<Organization> getAllOrganizations() {
//        return organizationRepository.findAll();
//    }

    // feature request: be able to add users by email, to an organization
    // try to get "multi-tenant-add-users.html" to work
}
