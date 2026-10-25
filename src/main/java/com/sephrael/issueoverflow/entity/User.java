package com.sephrael.issueoverflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
@Table(name = "users")
@Audited
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(unique = true)
    @NotNull
    @NotEmpty
    private String email;

    @Column
    @JsonIgnore
    @NotNull
    @NotEmpty
    @NotAudited
    private String password;

    @Column
    @NotNull
    @NotEmpty
    private String firstName;

    @Column
    @NotNull
    @NotEmpty
    private String lastName;

    @NotAudited
    private boolean enabled;

    @ManyToOne
    @NotAudited
    private Role role;

    @ManyToOne
    @NotAudited
    private Organization organization;

    @ManyToMany(mappedBy = "users")
    @NotAudited
    private List<Project> projects = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @NotAudited
    private List<Issue> issues;

    public void addToIssue(Issue issue) {
        issue.setUser(this);
    }

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @NotAudited
    private List<AWSFile> awsFiles;

    public void addUserToAWSFile(AWSFile awsFile) {
        awsFile.setUser(this);
    }

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @NotAudited
    private List<Comment> comments;

    public void addToComment(Comment comment) {
        comment.setUser(this);
    }

    @NotAudited
    @Column(unique = true)
    private String resetPasswordToken;

    @NotAudited
    private boolean hasProfilePicture;

    @NotAudited
    private String profilePictureKey;

    @NotAudited
    private boolean isCreatedEnabled;

    @NotAudited
    private boolean isUpdatedEnabled;

    @NotAudited
    private boolean isCommentsEnabled;

    @NotAudited
    private boolean isAllIssuesEnabled;

    @NotAudited
    private boolean isAssignedIssuesEnabled;

    @NotAudited
    private boolean isAuthoredIssuesEnabled;

    @Transient
    private boolean isNotificationsEnabled;

    @NotAudited
    private boolean isEmailVerified;

    @NotAudited
    @Column(unique = true)
    private String emailVerificationToken;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public List<Project> getProjects() {
        // sorts the Projects alphabetically by their Names
        projects.sort(Comparator.comparing(Project::getName));

        return projects;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
    }

    public String getResetPasswordToken() {
        return resetPasswordToken;
    }

    public void setResetPasswordToken(String resetPasswordToken) {
        this.resetPasswordToken = resetPasswordToken;
    }

    public boolean hasProfilePicture() {
        return hasProfilePicture;
    }

    public void setHasProfilePicture(boolean hasProfilePicture) {
        this.hasProfilePicture = hasProfilePicture;
    }

    public String getProfilePictureKey() {
        return profilePictureKey;
    }

    public void setProfilePictureKey(String profilePictureKey) {
        this.profilePictureKey = profilePictureKey;
    }

    public boolean isCreatedEnabled() {
        return isCreatedEnabled;
    }

    public void setIsCreatedEnabled(boolean isCreatedEnabled) {
        this.isCreatedEnabled = isCreatedEnabled;
    }

    public boolean isUpdatedEnabled() {
        return isUpdatedEnabled;
    }

    public void setIsUpdatedEnabled(boolean isUpdatedEnabled) {
        this.isUpdatedEnabled = isUpdatedEnabled;
    }

    public boolean isCommentsEnabled() {
        return isCommentsEnabled;
    }

    public void setIsCommentsEnabled(boolean isCommentsEnabled) {
        this.isCommentsEnabled = isCommentsEnabled;
    }

    public boolean isAllIssuesEnabled() {
        return isAllIssuesEnabled;
    }

    public void setIsAllIssuesEnabled(boolean isAllIssuesEnabled) {
        this.isAllIssuesEnabled = isAllIssuesEnabled;
    }

    public boolean isAssignedIssuesEnabled() {
        return isAssignedIssuesEnabled;
    }

    public void setIsAssignedIssuesEnabled(boolean isAssignedIssuesEnabled) {
        this.isAssignedIssuesEnabled = isAssignedIssuesEnabled;
    }

    public boolean isAuthoredIssuesEnabled() {
        return isAuthoredIssuesEnabled;
    }

    public void setIsAuthoredIssuesEnabled(boolean isAuthoredIssuesEnabled) {
        this.isAuthoredIssuesEnabled = isAuthoredIssuesEnabled;
    }

    public boolean isNotificationsEnabled() {
        return isCreatedEnabled || isUpdatedEnabled || isCommentsEnabled || isAllIssuesEnabled ||
                isAssignedIssuesEnabled || isAuthoredIssuesEnabled;
    }

    public boolean isEmailVerified() {
        return isEmailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        isEmailVerified = emailVerified;
    }

    public String getEmailVerificationToken() {
        return emailVerificationToken;
    }

    public void setEmailVerificationToken(String emailVerificationToken) {
        this.emailVerificationToken = emailVerificationToken;
    }
}
