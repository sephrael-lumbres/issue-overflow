package com.sephrael.issueoverflow.service;

import com.sephrael.issueoverflow.configuration.AppProperties;
import com.sephrael.issueoverflow.entity.*;
import com.sephrael.issueoverflow.repository.IssueRepository;
import com.sephrael.issueoverflow.repository.RoleRepository;
import com.sephrael.issueoverflow.repository.UserRepository;
import com.sephrael.issueoverflow.service.mail.Email;
import com.sephrael.issueoverflow.service.mail.EmailSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class UserService {

    private static final String SENDER_NAME = "Issue Overflow";
    private static final String SUPPORT_ADDRESS = "support@issueoverflow.app";
    private static final String NOTIFICATIONS_ADDRESS = "notifications@issueoverflow.app";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private IssueRepository issueRepository;
    @Autowired
    private EmailSender emailSender;
    @Autowired
    private EmailLinks emailLinks;
    @Autowired
    private AppProperties appProperties;

    public void registerDefaultUser(User user) {
        Role role = roleRepository.findByName("Guest");
        role.addToUser(user);
        user.setEnabled(true);

        userRepository.save(user);
    }

    public void updateResetPasswordToken(String token, String email) {
        User user = userRepository.findByEmail(email);

        if(user != null) {
            user.setResetPasswordToken(TokenGenerator.hash(token));
            user.setResetPasswordTokenExpiresAt(LocalDateTime.now().plus(appProperties.passwordResetTtl()));
            userRepository.save(user);
        }
    }

    // The user a reset link belongs to, or null when the token is unknown, used or expired.
    public User getUserByResetPasswordToken(String token) {
        if(!StringUtils.hasText(token))
            return null;

        User user = userRepository.findByResetPasswordToken(TokenGenerator.hash(token));

        if(user == null || isExpired(user.getResetPasswordTokenExpiresAt()))
            return null;

        return user;
    }

    // Marks the email address verified if the token is valid, and uses the token up. Returns whether it was valid.
    public boolean verifyEmail(String token) {
        if(!StringUtils.hasText(token))
            return false;

        User user = userRepository.findByEmailVerificationToken(TokenGenerator.hash(token));

        if(user == null || isExpired(user.getEmailVerificationTokenExpiresAt()))
            return false;

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiresAt(null);
        userRepository.save(user);

        return true;
    }

    private static boolean isExpired(LocalDateTime expiresAt) {
        return expiresAt == null || !expiresAt.isAfter(LocalDateTime.now());
    }

    public void sendPasswordResetEmail(String recipientEmail, String token) {
        String link = HtmlUtils.htmlEscape(emailLinks.passwordReset(token));
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

        emailSender.send(new Email(SUPPORT_ADDRESS, SENDER_NAME, List.of(recipientEmail), subject, content));
    }

    public void updatePassword(User user, String newPassword) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiresAt(null);
        userRepository.save(user);
    }

    public void sendVerificationEmail(User user) {
        String emailVerificationToken = TokenGenerator.newToken();
        String verifyEmailLink = HtmlUtils.htmlEscape(emailLinks.emailVerification(emailVerificationToken));

        user.setEmailVerificationToken(TokenGenerator.hash(emailVerificationToken));
        user.setEmailVerificationTokenExpiresAt(LocalDateTime.now().plus(appProperties.emailVerificationTtl()));
        userRepository.save(user);

        String subject = "Here is the link to verify your email address";
        String content = "<p>Hello " + HtmlUtils.htmlEscape(user.getFullName()) + ",</p>"
                + "<p>You have requested to verify your email address to receive notifications.</p>"
                + "<p>Click the link below to verify your email address:</p>"
                + "<p><a href=\"" + verifyEmailLink + "\">Verify Email</a></p>"
                + "<br>"
                + "<p>Or, copy and paste this link into your browser:</p>"
                + "<p><a href=\"" + verifyEmailLink + "\">" + verifyEmailLink + "</a></p>"
                + "<br>"
                + "<p>Ignore this email if you have not made this request.</p>";

        emailSender.send(new Email(SUPPORT_ADDRESS, SENDER_NAME, List.of(user.getEmail()), subject, content));
    }

    // The project members to notify: those who verified their address, want this kind of event (created, updated or commented),
    // and want it for this issue (all issues, or the ones they wrote or are assigned to, by whichever boxes they ticked).
    public List<String> getEmailRecipients(List<User> users, Issue issue, boolean isCreatedIssue, boolean isComment) {
        List<String> recipients = new ArrayList<>();

        for(User user : users) {
            boolean wantsThisNotification;

            if(isComment)
                wantsThisNotification = user.isCommentsEnabled();
            else if(isCreatedIssue)
                wantsThisNotification = user.isCreatedEnabled();
            else
                wantsThisNotification = user.isUpdatedEnabled();

            if(wantsThisNotification && user.isEmailVerified() && wantsThisIssueNotification(user, issue))
                recipients.add(user.getEmail());
        }

        return recipients;
    }

    private static boolean wantsThisIssueNotification(User user, Issue issue) {
        return user.isAllIssuesEnabled()
                || (user.isAuthoredIssuesEnabled() && isSameUser(issue.getUser(), user))
                || (user.isAssignedIssuesEnabled() && isSameUser(issue.getAssignedTo(), user));
    }

    private static boolean isSameUser(User user, User other) {
        return user != null && other != null && Objects.equals(user.getId(), other.getId());
    }

    public void sendEmailNotification(Issue issue, Comment comment, boolean isCreatedIssue, boolean isComment) {
        List<User> users = issue.getProject().getUsers();
        // names, titles and keys are user input: escape them, as the HTML body would render them
        String link = HtmlUtils.htmlEscape(emailLinks.issue(issue));
        String issueKey = HtmlUtils.htmlEscape(issue.getIssueKey());
        String content;

        List<String> recipients = getEmailRecipients(users, issue, isCreatedIssue, isComment);
        if(!recipients.isEmpty()) {
            String subject = "[" + issue.getIssueKey() + "] " + issue.getTitle();

            // if notification is for a newly created issue
            if (isCreatedIssue && !isComment) {
                content = "<h2>" + HtmlUtils.htmlEscape(issue.getUser().getFullName()) + " added a new issue to "
                        + HtmlUtils.htmlEscape(issue.getProject().getName()) + ".</h2>"
                        + "<p>Click the link below to view the full details of this issue:</p>"
                        + "<p><a href=\"" + link + "\">" + link + "</a></p>"
                        + "<br>"
                        + "<p>Or, copy and paste this link into your browser:</p>"
                        + "<p><a href=\"" + link + "\">" + link + "</a></p>";
                // else if notification is for an updated issue
            } else if (!isCreatedIssue && !isComment) {
                content = "<h2>Details for " + issueKey + " have been updated by "
                        + HtmlUtils.htmlEscape(issue.getUpdatedBy().getFullName()) + ".</h2>"
                        + "<p>Click the link below to view the updated details of this issue:</p>"
                        + "<p><a href=\"" + link + "\">" + link + "</a></p>"
                        + "<br>"
                        + "<p>Or, copy and paste this link into your browser:</p>"
                        + "<p><a href=\"" + link + "\">" + link + "</a></p>"
                        + "<p>Updated: " + issue.getDateUpdated().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss")) + "</p>";
                // else, the notification is for a new comment
            } else {
                content = "<h2>" + HtmlUtils.htmlEscape(comment.getUser().getFullName()) + " added a new comment to " + issueKey + ".</h2>"
                        + "<p>Click the link below to view the full details of this issue:</p>"
                        + "<p><a href=\"" + link + "\">" + link + "</a></p>"
                        + "<br>"
                        + "<p>Or, copy and paste this link into your browser:</p>"
                        + "<p><a href=\"" + link + "\">" + link + "</a></p>";
            }

            emailSender.send(new Email(NOTIFICATIONS_ADDRESS, SENDER_NAME, recipients, subject, content));
        }
    }

    public void manageUserProjects(User userToBeUpdated, List<Project> projects) {
        // adds User to selected Projects if the User was not already involved
        for(Project project : projects) {

            if(!project.getUsers().contains(userToBeUpdated)) {
                project.addUser(userToBeUpdated);
            }
        }

        // removes User from unselected Projects if the User was previously involved
        for(Project project : userToBeUpdated.getOrganization().getProjects()) {
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
