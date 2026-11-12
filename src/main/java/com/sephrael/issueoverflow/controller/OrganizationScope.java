package com.sephrael.issueoverflow.controller;

import com.sephrael.issueoverflow.entity.Organization;
import com.sephrael.issueoverflow.entity.Project;
import com.sephrael.issueoverflow.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

/**
 * Tenancy checks shared by the controllers. A record from another organization gets the same answer as the
 * one that doesn't exist: the 404 page. The file route (GET /storage/files/{fileKey}) already behaves this way.
 * Public so the JSON controllers in controller.api use the same checks (there the 404 is a problem-details body).
 */
public final class OrganizationScope {

    private OrganizationScope() {
    }

    public static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    public static boolean belongsTo(Organization organization, Organization expected) {
        return organization != null && expected != null && Objects.equals(organization.getId(), expected.getId());
    }

    // Project members see a project's issues, attachments and pages; to anyone else in the organization they look missing.
    public static boolean isMember(User user, Project project) {
        return user != null && project != null && user.getProjects() != null
                && user.getProjects().stream().anyMatch(member -> Objects.equals(member.getId(), project.getId()));
    }
}
