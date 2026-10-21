package com.sephrael.issueoverflow.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Audited
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @NotNull
    private String name;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDate dateCreated;

    @Column
    @NotNull
    private String identifier;

    @NotNull
    private String color;

    @ManyToMany
    @JsonIgnore
    @NotAudited
    @JoinTable(
            name = "project_user",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<User> users = new ArrayList<>();

    @ManyToOne
    @JsonIgnore
    @NotAudited
    private Organization organization;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    @JsonIgnore
    @NotAudited
    private List<Issue> issues;

    @NotAudited
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
    private List<AWSFile> awsFiles;

    public void addAWSFileToProject(AWSFile awsFile) {
        awsFile.setProject(this);
    }

    public List<Issue> getIssues() {
        return issues;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public void addUser(User user) {
        this.users.add(user);
//        user.getProjects().add(this);
    }

    public void removeUserFromProject(User user) {
        this.users.remove(user);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDate dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier.toUpperCase();
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}