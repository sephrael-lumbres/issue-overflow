package com.sephrael.issueoverflow.controller;

import com.sephrael.issueoverflow.repository.UserRepository;
import com.sephrael.issueoverflow.service.mail.Email;
import com.sephrael.issueoverflow.service.mail.EmailSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.HtmlUtils;

import java.security.Principal;
import java.util.List;

@Controller
public class HelpController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmailSender emailSender;

    @RequestMapping("/getting-started")
    public String showGettingStartedCategory(Model model, Principal principal) {
        if(principal != null)
            model.addAttribute("currentUser", userRepository.findByEmail(principal.getName()));

        return "knowledge-base/getting-started/getting-started-category";
    }

    @RequestMapping("/getting-started/project-manager")
    public String showGettingStartedPageForProjectManager(Model model, Principal principal) {
        if(principal != null)
            model.addAttribute("currentUser", userRepository.findByEmail(principal.getName()));

        return "knowledge-base/getting-started/articles/project-manager-guide";
    }

    @RequestMapping("/getting-started/user")
    public String showGettingStartedForUserPage(Model model, Principal principal) {
        if(principal != null)
            model.addAttribute("currentUser", userRepository.findByEmail(principal.getName()));

        return "knowledge-base/getting-started/articles/user-guide";
    }

    @RequestMapping("/contact-us")
    public String showContactUsPage(Model model, Principal principal) {
        if(principal != null)
            model.addAttribute("currentUser", userRepository.findByEmail(principal.getName()));

        return "help/contact-us";
    }

    @PostMapping("/contact-us/send-message")
    public String sendMessage(@RequestParam("name") String name, @RequestParam("email") String email,
                              @RequestParam("message") String message, RedirectAttributes redirectAttributes) {
        String subject = "Contact Us - Message from: " + name;

        // the body is HTML and every field comes from an anonymous form: escape them before adding line breaks
        message = HtmlUtils.htmlEscape(message).replaceAll("(\r\n|\n)", "<br>");
        message = message.concat("<br><br>From: " + HtmlUtils.htmlEscape(name) + " <br>Email address: " + HtmlUtils.htmlEscape(email));

        // sent after the response; a failed send is logged (EmailSender)
        emailSender.send(new Email("support@issueoverflow.app", "Issue Overflow", List.of("support@issueoverflow.app"), subject, message));
        redirectAttributes.addFlashAttribute("success", "Your message has been sent!");

        return "redirect:/contact-us";
    }

    @RequestMapping("/privacy")
    public String showPrivacyPolicyPage(Principal principal, Model model) {
        if(principal != null)
            model.addAttribute("currentUser", userRepository.findByEmail(principal.getName()));

        return "help/privacy-policy";
    }
}
