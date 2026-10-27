package com.sephrael.issueoverflow.suite;

import com.sephrael.issueoverflow.entity.Organization;
import com.sephrael.issueoverflow.repository.OrganizationRepository;
import com.sephrael.issueoverflow.support.RepositoryTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationRepositoryTests extends RepositoryTestBase {

    @Autowired
    private OrganizationRepository organizationRepository;

    private Organization savedOrganization() {
        return organizationRepository.save(newOrganization("Call of Duty", "COD"));
    }

    @Test
    void testCreateOrganization() {
        Organization organization = savedOrganization();
        flushAndClear();

        Organization existingOrganization = entityManager.find(Organization.class, organization.getId());

        assert existingOrganization != null;
        assertThat(existingOrganization.getAccessKey()).isEqualTo("COD");
    }

    @Test
    void testFindOrganizationByAccessKey() {
        savedOrganization();
        flushAndClear();

        Organization organization = organizationRepository.findByAccessKey("COD");

        assertThat(organization.getName()).isEqualTo("Call of Duty");
    }

    @Test
    void testListOrganizations() {
        Organization organization = savedOrganization();
        flushAndClear();

        assertThat(organizationRepository.findAll())
                .extracting(Organization::getId)
                .contains(organization.getId());
    }

    @Test
    void testUpdateOrganization() {
        savedOrganization();
        flushAndClear();

        Organization organization = organizationRepository.findByAccessKey("COD");
        organization.setName("Activision");
        organizationRepository.save(organization);
        flushAndClear();

        assertThat(organizationRepository.findByAccessKey("COD").getName()).isEqualTo("Activision");
    }

    @Test
    void testDeleteOrganization() {
        Organization organization = savedOrganization();
        flushAndClear();

        organizationRepository.deleteById(organization.getId());
        flushAndClear();

        assertThat(organizationRepository.findByAccessKey("COD")).isNull();
    }
}
