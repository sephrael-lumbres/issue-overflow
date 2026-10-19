package com.sephrael.issueoverflow.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.data.history.RevisionMetadata;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Audited
public class Issue {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String type;

    private String priority;

    private String status;

    private Integer estimatedHours;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    private String issueKey;

    @Transient
    private RevisionMetadata<Integer> changeVersion;

    @Transient
    private String property;

    @Transient
    private String oldValue;

    @Transient
    private String newValue;

    @CreationTimestamp
    @JsonFormat(pattern = "dd MMM yyyy HH:mm:ss")
    @Column(updatable = false)
    @NotAudited
    private LocalDateTime dateCreated;

    @UpdateTimestamp
    @NotAudited
    @JsonFormat(pattern = "dd MMM yyyy HH:mm:ss")
    private LocalDateTime dateUpdated;

    @NotAudited
    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL)
    private List<AWSFile> awsFiles;

    public void addAWSFileToIssue(AWSFile awsFile) {
        awsFile.setIssue(this);
    }

    public List<AWSFile> getAWSFiles() {
        return awsFiles;
    }

//    @Enumerated(EnumType.STRING)
//    private IssueStatus issueStatus;

    @NotAudited
    @ManyToOne
    private Organization organization;

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    @ManyToOne
    @NotNull
    @JoinColumn(updatable = false)
    @JsonIgnore
    @NotAudited
    private User user;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @ManyToOne
    @JsonIgnore
    private User updatedBy;

    public User getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
    }

    @ManyToOne
    @JsonIgnore
    private User assignedTo;

    public User getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(User assignedTo) {
        this.assignedTo = assignedTo;
    }

    @ManyToOne
    @NotNull
//    @JsonIgnore
    private Project project;

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    @NotAudited
    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL)
    private List<Comment> comments;

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public void addToComment(Comment comment) {
        comment.setIssue(this);
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(Integer estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getIssueKey() {
        return issueKey;
    }

    public void setIssueKey(String issueKey) {
        this.issueKey = issueKey;
    }

    public RevisionMetadata<Integer> getChangeVersion() {
        return changeVersion;
    }

    public void setChangeVersion(RevisionMetadata<Integer> changeVersion) {
        this.changeVersion = changeVersion;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public LocalDateTime getDateUpdated() {
        return dateUpdated;
    }

    public void setDateUpdated(LocalDateTime dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    //    public IssueStatus getIssueStatus() {
//        return issueStatus;
//    }
//
//    public void setIssueStatus(IssueStatus issueStatus) {
//        this.issueStatus = issueStatus;
//    }
}

