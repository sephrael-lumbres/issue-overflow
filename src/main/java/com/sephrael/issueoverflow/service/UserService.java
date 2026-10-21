package com.sephrael.issueoverflow.service;

import com.sephrael.issueoverflow.entity.*;
import com.sephrael.issueoverflow.repository.IssueRepository;
import com.sephrael.issueoverflow.repository.RoleRepository;
import com.sephrael.issueoverflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private IssueRepository issueRepository;
    @Autowired
    private JavaMailSender mailSender;

    public void registerDefaultUser(User user) {
        Role role = roleRepository.findByName("Guest");
        role.addToUser(user);
        user.setEnabled(true);

        userRepository.save(user);
    }

    public void updateResetPasswordToken(String token, String email) {
        User user = userRepository.findByEmail(email);

        if(user != null) {
            user.setResetPasswordToken(token);
            userRepository.save(user);
        }
    }

    public User getUserByResetPasswordToken(String token) {
        return userRepository.findByResetPasswordToken(token);
    }

    public void sendPasswordResetEmail(String recipientEmail, String link) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom("support@issueoverflow.app", "Issue Overflow");
        helper.setTo(recipientEmail);

        String subject = "Here is the link to reset your password";

        String content = "<p>Hello,</p>"
                + "<p>You have requested to reset your password.</p>"
                + "<p>Click the link below to reset and change your password:</p>"
                + "<p><a href=\"" + link + "\">Reset password</a></p>"
                + "<br>"
                + "<p>Or, copy and paste this link into your browser:</p>"
                + "<p><a href=\"" + link + "\">" + link + "</a></p>"
                + "<br>"
                + "<p>Ignore this email if you do remember your password, "
                + "or if you have not made this request.</p>";

        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }

    public void updatePassword(User user, String newPassword) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        user.setResetPasswordToken(null);

        userRepository.save(user);
    }

    public void sendVerificationEmail(User user, HttpServletRequest request) throws MessagingException, UnsupportedEncodingException {
        String emailVerificationToken = TokenGenerator.newToken();
        String siteURL = request.getRequestURL().toString();
        String verifyEmailLink = siteURL.replace(request.getServletPath(), "") + "/verify-email?emailVerificationToken=" + emailVerificationToken;

        user.setEmailVerificationToken(emailVerificationToken);
        userRepository.save(user);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom("support@issueoverflow.app", "Issue Overflow");
        helper.setTo(user.getEmail());

        String subject = "Here is the link to verify your email address";

        String content = "<p>Hello " + user.getFullName() + ",</p>"
                + "<p>You have requested to verify your email address to receive notifications.</p>"
                + "<p>Click the link below to verify your email address:</p>"
                + "<p><a href=\"" + verifyEmailLink + "\">Verify Email</a></p>"
                + "<br>"
                + "<p>Or, copy and paste this link into your browser:</p>"
                + "<p><a href=\"" + verifyEmailLink + "\">" + verifyEmailLink + "</a></p>"
                + "<br>"
                + "<p>Ignore this email if you have not made this request.</p>";

        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }

    // checks which users agreed to receive notifications and adds them to the email recipient list
    public void checkEmailRecipients(User user, Issue issue, String[] emailRecipients, int i) {
        // checks if user agreed to send notifications of ALL created issues
        if(user.isAllIssuesEnabled()) {
            emailRecipients[i] = user.getEmail();

            // checks if user agreed to send notifications of ONLY created issues CREATED BY THEM
        } else if(user.isAuthoredIssuesEnabled()) {
            if(issue.getUser() == user) {
                emailRecipients[i] = user.getEmail();
            }

            // checks if user agreed to send notifications of ONLY created issues ASSIGNED TO THEM
        } else if(user.isAssignedIssuesEnabled()) {
            if(issue.getAssignedTo() == user) {
                emailRecipients[i] = user.getEmail();
            }
        }
    }

    public String[] getEmailRecipients(List<User> users, Issue issue, boolean isCreatedIssue, boolean isComment) {
        String[] emailRecipients = new String[users.size()];

        // if notification is for a newly created issue
        if(isCreatedIssue && !isComment) {
            for(int i=0; i < users.size(); i++) {
                User user = users.get(i);

                // checks if user agreed to send notifications of created issues and if the user verified their email address
                if(user.isCreatedEnabled() && user.isEmailVerified()) {
                    // checks which users agreed to receive notifications and adds them to the email recipient list
                    checkEmailRecipients(user, issue, emailRecipients, i);
                }
            }

            // else if notification is for an updated issue
        } else if(!isCreatedIssue && !isComment) {
            for(int i=0; i < users.size(); i++) {
                User user = users.get(i);

                // checks if user agreed to send notifications of updated issues and if the user verified their email address
                if(user.isUpdatedEnabled() && user.isEmailVerified()) {
                    // checks which users agreed to receive notifications and adds them to the email recipient list
                    checkEmailRecipients(user, issue, emailRecipients, i);
                }
            }

            // else, the notification is for a new comment
        } else {
            for(int i=0; i < users.size(); i++) {
                User user = users.get(i);

                // checks if user agreed to send notifications of added comments and if the user verified their email address
                if(user.isCommentsEnabled() && user.isEmailVerified()) {
                    // checks which users agreed to receive notifications and adds them to the email recipient list
                    checkEmailRecipients(user, issue, emailRecipients, i);
                }
            }
        }

        return emailRecipients;
    }

    public void sendEmailNotification(Issue issue, Comment comment, HttpServletRequest request, boolean isCreatedIssue,
                                      boolean isComment) throws MessagingException, UnsupportedEncodingException {
        List<User> users = issue.getProject().getUsers();
        String rootURL = request.getRequestURL().toString();
        String link = rootURL.replace(request.getServletPath(), "") + "/issues/" + issue.getProject().getIdentifier() + "/view/" + issue.getIssueKey();
        String content;

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom("notifications@issueoverflow.app", "Issue Overflow");

        if(getEmailRecipients(users, issue, isCreatedIssue, isComment)[0] != null) {
            helper.setTo(getEmailRecipients(users, issue, isCreatedIssue, isComment));

            String subject = "[" + issue.getIssueKey() + "] " + issue.getTitle();
            helper.setSubject(subject);

            // if notification is for a newly created issue
            if (isCreatedIssue && !isComment) {
                content = "<h2>" + issue.getUser().getFullName() + " added a new issue to " + issue.getProject().getName() + ".</h2>"
                        + "<p>Click the link below to view the full details of this issue:</p>"
                        + "<p><a href=\"" + link + "\">" + link + "</a></p>"
                        + "<br>"
                        + "<p>Or, copy and paste this link into your browser:</p>"
                        + "<p><a href=\"" + link + "\">" + link + "</a></p>";
                // else if notification is for an updated issue
            } else if (!isCreatedIssue && !isComment) {
                content = "<h2>Details for " + issue.getIssueKey() + " have been updated by " + issue.getUpdatedBy().getFullName() + ".</h2>"
                        + "<p>Click the link below to view the updated details of this issue:</p>"
                        + "<p><a href=\"" + link + "\">" + link + "</a></p>"
                        + "<br>"
                        + "<p>Or, copy and paste this link into your browser:</p>"
                        + "<p><a href=\"" + link + "\">" + link + "</a></p>"
                        + "<p>Updated: " + issue.getDateUpdated().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss")) + "</p>";
                // else, the notification is for a new comment
            } else {
                content = "<h2>" + comment.getUser().getFullName() + " added a new comment to " + comment.getIssue().getIssueKey() + ".</h2>"
                        + "<p>Click the link below to view the full details of this issue:</p>"
                        + "<p><a href=\"" + link + "\">" + link + "</a></p>"
                        + "<br>"
                        + "<p>Or, copy and paste this link into your browser:</p>"
                        + "<p><a href=\"" + link + "\">" + link + "</a></p>";

            }

            helper.setText(content, true);

            mailSender.send(message);
        }
    }

    public void manageUserProjects(User userToBeUpdated, User userBeforeUpdate, List<Project> projects) {
        // adds User to selected Projects if the User was not already involved
        for(Project project : projects) {

            if(!project.getUsers().contains(userToBeUpdated)) {
                project.addUser(userToBeUpdated);
            }
        }

        // removes User from unselected Projects if the User was previously involved
        for(Project project : userBeforeUpdate.getOrganization().getProjects()) {
            if(!projects.contains(project) && project.getUsers().contains(userToBeUpdated)) {
                project.removeUserFromProject(userToBeUpdated);
            }
        }
    }

    public void removeUserFromAllProjects(User userBeforeUpdate) {
        for(Project project : userBeforeUpdate.getOrganization().getProjects()) {
            if(project.getUsers().contains(userBeforeUpdate)) {
                project.removeUserFromProject(userBeforeUpdate);
            }
        }
    }

    public void unassignAllIssuesBeforeUserDeletion(User userBeforeDeletion) {
        for(Issue issue : issueRepository.findByOrganization(userBeforeDeletion.getOrganization())) {
            if(issue.getAssignedTo() != null && Objects.equals(issue.getAssignedTo().getId(), userBeforeDeletion.getId())) {
                issue.setAssignedTo(null);
            }
        }
    }

    public void deleteAllUserIssues(User user) {
        for(Issue issue : issueRepository.findByUser(user)) {
            issueRepository.deleteById(issue.getId());
        }
    }

    public List<Role> listRoles() {
        return roleRepository.findAll();
    }
}
