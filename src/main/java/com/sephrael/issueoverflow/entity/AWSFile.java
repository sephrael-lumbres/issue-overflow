package com.sephrael.issueoverflow.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class AWSFile {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String fileName;

    @Column(nullable = false, unique = true)
    private String fileKey;

    private String type;

    private long size;

    private boolean isProfilePicture;

    public AWSFile() {}

    public AWSFile(String fileName, String fileKey, String type, long size, boolean isProfilePicture) {
        this.fileName = fileName;
        this.fileKey = fileKey;
        this.type = type;
        this.size = size;
        this.isProfilePicture = isProfilePicture;
    }

    @CreationTimestamp
    @JsonFormat(pattern = "dd MMM yyyy HH:mm:ss")
    @Column(updatable = false)
    private LocalDateTime dateCreated;

    @ManyToOne
    @JsonIgnore
    private User user;

    @ManyToOne
    @JsonIgnore
    private Project project;

    @ManyToOne
    @JsonIgnore
    private Issue issue;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileKey() {
        return fileKey;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isProfilePicture() {
        return isProfilePicture;
    }

    public void setProfilePicture(boolean profilePicture) {
        isProfilePicture = profilePicture;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Issue getIssue() {
        return issue;
    }

    public void setIssue(Issue issue) {
        this.issue = issue;
    }
}
