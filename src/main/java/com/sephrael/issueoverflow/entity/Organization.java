package com.sephrael.issueoverflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String name;

    // insert dateOrganizationJoined variable here (a somewhat working Date variable in Issue.java)

    @JsonIgnore
    @OneToMany(mappedBy = "organization")
    private List<User> users;

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public void addToUser(User user) {
        user.setOrganization(this);
//        this.users.add(user);
    }

    public void removeUser(User user) {
        this.users.remove(user);
        user.setOrganization(null);
    }

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL)
    private List<Project> projects;

    public List<Project> getProjects() {
        return projects;
    }

    public void addToProject(Project project) {
        project.setOrganization(this);
    }

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL)
    private List<Issue> issues;

    public void addToIssue(Issue issue) {
        issue.setOrganization(this);
    }

    @Column(unique = true, updatable = false)
    private String accessKey;

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

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }
}
