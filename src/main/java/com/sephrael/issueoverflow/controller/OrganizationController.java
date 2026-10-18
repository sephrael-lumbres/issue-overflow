package com.sephrael.issueoverflow.controller;

import com.sephrael.issueoverflow.entity.Organization;
import com.sephrael.issueoverflow.entity.Project;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.repository.OrganizationRepository;
import com.sephrael.issueoverflow.repository.RoleRepository;
import com.sephrael.issueoverflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Random;

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

    public static String generateRandomAccessKey(int len) {
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            stringBuilder.append(chars.charAt(random.nextInt(chars.length())));
        return stringBuilder.toString();
    }

    @PostMapping("/save")
    public String createOrganization(Organization organization, Principal principal,
                                     HttpServletRequest request,HttpServletResponse response) {
        User currentUser = userRepository.findByEmail(principal.getName());
        String accessKey = generateRandomAccessKey(10);
        organization.setAccessKey(accessKey);

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

        if(currentUser.getOrganization() == null)
            return "organization/select-organization";

        try {
            Organization previousOrganization = organizationRepository.findOrganizationById(id);
            previousOrganization.setName(organizationName);

            organizationRepository.save(previousOrganization);

            redirectAttributes.addFlashAttribute("updateOrganizationSuccess", "Organization has been successfully updated!");
        } catch(Exception exception) {
            redirectAttributes.addFlashAttribute("updateOrganizationError", "An error has occurred while attempting to save Organization changes");;
        }

        return "redirect:/organization";
    }

    @PostMapping("/joining")
    public String joinOrganization(@RequestParam(value = "accessKey") String accessKey, Principal principal,
                                   HttpServletRequest request, HttpServletResponse response) {
        User currentUser = userRepository.findByEmail(principal.getName());
        Organization organization = organizationRepository.findByAccessKey(accessKey);

        // if the Organization that was requested to be joined, has ZERO members, grant the User with the Project Manager role
        if(organization.getUsers().size() == 0) {
            roleRepository.findByName("Project Manager").addToUser(currentUser);
            userRepository.save(currentUser);
        }

        // this gets the access key input by the user from the "Join Organization Form" and uses the access key to find
        // the associated Organization and adds the current user to the desired Organization
        organizationRepository.findByAccessKey(accessKey).addToUser(currentUser);

        // after adding the current user to the desired Organization, this saves the Organization
        organizationRepository.save(organizationRepository.findByAccessKey(accessKey));

        // logs out the Current User to allow the Authorization of 'Project Manager'
        if(organization.getUsers().size() == 0)
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
