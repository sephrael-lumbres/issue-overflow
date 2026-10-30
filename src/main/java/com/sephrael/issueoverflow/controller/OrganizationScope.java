package com.sephrael.issueoverflow.controller;

import com.sephrael.issueoverflow.entity.Organization;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

/**
 * Tenancy checks shared by the controllers. A record from another organization gets the same answer as the
 * one that doesn't exist: the 404 page. The file route (GET /storage/files/{fileKey}) already behaves this way.
 */
final class OrganizationScope {

    private OrganizationScope() {
    }

    static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    static boolean belongsTo(Organization organization, Organization expected) {
        return organization != null && expected != null && Objects.equals(organization.getId(), expected.getId());
    }
}
