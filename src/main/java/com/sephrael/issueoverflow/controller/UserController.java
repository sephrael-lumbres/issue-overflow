package com.sephrael.issueoverflow.controller;

import com.sephrael.issueoverflow.controller.form.Passwords;
import com.sephrael.issueoverflow.controller.form.RegistrationForm;
import com.sephrael.issueoverflow.entity.Organization;
import com.sephrael.issueoverflow.entity.Project;
import com.sephrael.issueoverflow.entity.Role;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.repository.*;
import com.sephrael.issueoverflow.service.AWSFileService;
import com.sephrael.issueoverflow.service.TokenGenerator;
import com.sephrael.issueoverflow.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Objects;

@Controller
public class UserController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private AWSFileService awsFileService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());

        return "register";
    }

    @PostMapping("/process_register")
    public String processRegister(@Valid RegistrationForm form, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if(bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("registrationError", "Please fill in every field with a valid email address.");
            return "redirect:/register";
        }

        String passwordErrorMessage = Passwords.errorMessage(form.password());
        if(passwordErrorMessage != null) {
            redirectAttributes.addFlashAttribute("registrationError", passwordErrorMessage);
            return "redirect:/register";
        }

        // Said plainly, unlike the reset page: new accounts are enabled at once, so logging in would reveal it anyway.
        if(userRepository.findByEmail(form.email()) != null) {
            redirectAttributes.addFlashAttribute("registrationError", "An account with this email address already exists. " +
                    "Log in, or reset your password if you've forgotten it.");
            return "redirect:/register";
        }

        // only the form's fields reach the new account; registerDefaultUser sets the role and enables it
        User user = new User();
        user.setFirstName(form.firstName());
        user.setLastName(form.lastName());
        user.setEmail(form.email());

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode(form.password());
        user.setPassword(encodedPassword);

        userService.registerDefaultUser(user);
        redirectAttributes.addFlashAttribute("registered", "Your account is ready. Please log in.");

        return "redirect:/login";
    }

    @GetMapping("/login")
    public ModelAndView login(){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("login");

        return modelAndView;
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm() {
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("email") String email, Model model) {
        // The page says the same thing whether or not the address has an account, so it can't be used to find out who
        // is registered. The email is sent after the response (EmailSender), so the answer doesn't wait for the mail
        // server, and a failed send is logged rather than shown.
        if(userRepository.findByEmail(email) != null) {
            String token = TokenGenerator.newToken();
            userService.updateResetPasswordToken(token, email);
            userService.sendPasswordResetEmail(email, token);
        }

        model.addAttribute("resetRequested", true);

        return "reset-password";
    }

    @GetMapping("/change-password")
    public String showChangePasswordForm(@Param(value = "token") String token, Model model, RedirectAttributes redirectAttributes) {
        User user = userService.getUserByResetPasswordToken(token);
        model.addAttribute("token", token);

        if(user == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid Token!");
            return "redirect:/login";
        }

        return "change-password";
    }

    @PostMapping("/change-password")
    public String processChangePassword(@RequestParam("password") String password, @RequestParam("confirmPassword") String confirmPassword,
                                        HttpServletRequest request, Model model, RedirectAttributes redirectAttributes) {
        String token = request.getParameter("token");

        User user = userService.getUserByResetPasswordToken(token);
        model.addAttribute("title", "Reset your password");

        if(user == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid Token!");
        } else if(!Objects.equals(password, confirmPassword)) {
            redirectAttributes.addFlashAttribute("passwordsDoNotMatch", "Passwords do not match. Please try again.");
            return "redirect:/change-password?token=" + token;
        } else if(Passwords.errorMessage(password) != null) {
            redirectAttributes.addFlashAttribute("passwordError", Passwords.errorMessage(password));
            return "redirect:/change-password?token=" + token;
        } else {
            userService.updatePassword(user, password);
            redirectAttributes.addFlashAttribute("success", "You have successfully changed your password!");
        }

        return "redirect:/login";
    }

    // all the members of an organization
    @GetMapping("/users")
    public String listUsers(Model model, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());

        if(currentUser.getOrganization() == null)
            return "organization/select-organization";

        model.addAttribute("listUsers", userRepository.findByOrganizationOrderByRoleId(currentUser.getOrganization()));
        model.addAttribute("newProject", new Project());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentUserProjects", currentUser.getProjects());

        return "users/users-by-organization";
    }

    @GetMapping("/users/edit/{id}")
    public String showEditUserPage(@PathVariable("id") long id, Model model, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());

        if(currentUser.getOrganization() == null)
            return "organization/select-organization";

        User user = userRepository.getReferenceById(id);

        // if the Current User is NOT in the same Organization as the Requested User, show the 404 page
        if(!currentUser.getOrganization().getUsers().contains(user))
            throw OrganizationScope.notFound();

        model.addAttribute("user", user);
        model.addAttribute("listRoles", userService.listRoles());
        model.addAttribute("currentUserProjects", currentUser.getProjects());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("editUser", userRepository.findUserById(id));
        model.addAttribute("allProjectsInOrganization", projectRepository.findAllProjectsByOrganizationId(userRepository.findUserById(id).getOrganization().getId()));

        return "users/edit-user";
    }

    // Allows for updating a User's Role and Projects(only Project Managers are allowed to make these changes)
    @PostMapping(value = "/users/update/{id}")
    public String saveUserChanges(@PathVariable("id") Long id, @RequestParam("role") long roleId, Principal principal,
                                  @RequestParam(value = "projects", required = false) List<Project> projects,
                                  RedirectAttributes redirectAttributes) {
        User currentUser = userRepository.findByEmail(principal.getName());

        if(currentUser.getOrganization() == null)
            return "organization/select-organization";

        User userToBeUpdated = userRepository.findUserById(id);

        // if the Current User is NOT in the same Organization as the Requested User, show the 404 page
        if(!currentUser.getOrganization().getUsers().contains(userToBeUpdated))
            throw OrganizationScope.notFound();

        // the project checkboxes send IDs; every project must belong to the caller's organization
        if(projects != null && projects.stream().anyMatch(project ->
                project == null || !OrganizationScope.belongsTo(project.getOrganization(), currentUser.getOrganization())))
            throw OrganizationScope.notFound();

        // changes a User's Role (an unknown role id, which the form never sends, is a 404)
        Role role = roleRepository.findRoleById(roleId);

        if(role == null)
            throw OrganizationScope.notFound();

        role.addToUser(userToBeUpdated);

        if(projects != null) {
            // decides whether or not to add or remove a User from a Project
            userService.manageUserProjects(userToBeUpdated, projects);
        } else {
            // if all checkboxes for Projects are unchecked
            userService.removeUserFromAllProjects(userToBeUpdated);
        }

        userRepository.save(userToBeUpdated);
        redirectAttributes.addFlashAttribute("userUpdateSuccess", "Changes have been saved");

        return "redirect:/users";
    }

    // POST only, so the CSRF token protects it. The organization is the caller's own.
    @PostMapping("/users/remove/{id}")
    public String removeUserFromOrganization(@PathVariable("id") long id, Principal principal,
                                             HttpServletRequest request, HttpServletResponse response) {
        User currentUser = userRepository.findByEmail(principal.getName());
        Organization currentOrganization = currentUser.getOrganization();

        if(currentUser.getOrganization() == null)
            return "organization/select-organization";

        User userBeforeRemoval = userRepository.findUserById(id);

        // if the Current User is NOT in the same Organization as the Requested User, show the 404 page
        if(!currentUser.getOrganization().getUsers().contains(userBeforeRemoval))
            throw OrganizationScope.notFound();

        // deletes all the files associated with this user from AWS S3 bucket
        awsFileService.deleteAllFilesByUser(userBeforeRemoval);

        // deletes all the User's issues
        userService.deleteAllUserIssues(userBeforeRemoval);

        // unassigns a User from all Issues that they were previously assigned to
        userService.unassignAllIssuesBeforeUserDeletion(userBeforeRemoval);

        // if a User is involved with any Projects, this removes them from all those Projects before deleting the User
        if(userBeforeRemoval.getProjects() != null) {
            userService.removeUserFromAllProjects(userBeforeRemoval);
        }

        // removes User from Organization
        currentOrganization.removeUser(userBeforeRemoval);

        // deletes organization if there are no more members
        if(currentOrganization.getUsers().size() == 0)
            organizationRepository.deleteById(currentOrganization.getId());

        // resets the User's account, sets their role to 'Guest' and saves the changes
        userService.registerDefaultUser(userBeforeRemoval);

        if(Objects.equals(userBeforeRemoval.getId(), currentUser.getId()))
            new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());

        return "redirect:/users";
    }

    @RequestMapping("/verify-email")
    public String showEmailVerifiedPage(@Param(value = "emailVerificationToken") String emailVerificationToken, Model model) {
        // unknown, used and expired links all show the same "invalid link" page
        if(userService.verifyEmail(emailVerificationToken))
            model.addAttribute("userIsValid", true);
        else
            model.addAttribute("userIsNull", true);

        return "account-settings/verified-email";
    }

    // POST only: it replaces the verification token and sends an email.
    @PostMapping("/resend-verification-email")
    public String resendVerificationEmail(Principal principal, HttpServletRequest request, RedirectAttributes redirectAttributes) {

        try {
            User user = userRepository.findByEmail(principal.getName());

            userService.sendVerificationEmail(user);

            redirectAttributes.addFlashAttribute("verificationEmailSentSuccess", "Verification email has been sent!");
        } catch(Exception exception) {
            redirectAttributes.addFlashAttribute("verificationEmailSentError", "An error has occurred while attempting to send a verification email.");
        }

        return "redirect:" + sameSitePath(request.getHeader("Referer"));
    }

    // Back to the page the form was on. Only the path of the Referer is kept, so the redirect can't leave this site.
    static String sameSitePath(String referer) {
        if(referer != null) {
            try {
                String path = URI.create(referer).getRawPath();
                if(path != null && path.startsWith("/") && !path.startsWith("//"))
                    return path;
            } catch(IllegalArgumentException exception) {
                // not a valid URI; fall back to the dashboard
            }
        }
        return "/dashboard";
    }
}
