package com.sephrael.issueoverflow.repository;

import com.sephrael.issueoverflow.entity.Organization;
import com.sephrael.issueoverflow.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    Project findByIdentifierAndOrganization(String identifier, Organization organization);
    Project findProjectById(Long id);
    Project findByIdAndOrganization(Long id, Organization organization);
    List<Project> findAllProjectsByOrganizationId(Long id);

    // was previously used to access Project information but this method caused issues
    //@Query("SELECT p FROM Project p WHERE p.identifier = ?1")
    //Project findByIdentifier(String identifier);
}
