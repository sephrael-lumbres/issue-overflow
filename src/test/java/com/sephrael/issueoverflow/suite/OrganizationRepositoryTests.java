package com.sephrael.issueoverflow.suite;

import com.sephrael.issueoverflow.entity.Organization;
import com.sephrael.issueoverflow.repository.OrganizationRepository;
import com.sephrael.issueoverflow.support.PostgresTestBase;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.Rollback;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(value = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrganizationRepositoryTests extends PostgresTestBase {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private OrganizationRepository organizationRepository;

    @Test
    @Order(1)
    public void testCreateOrganization() {
        Organization organization = new Organization();
        organization.setName("Call of Duty");
        organization.setAccessKey("COD");

        Organization savedOrganization = organizationRepository.save(organization);

        Organization existingOrganization = entityManager.find(Organization.class, savedOrganization.getId());

        assertThat(organization.getAccessKey()).isEqualTo(existingOrganization.getAccessKey());
    }

    @Test
    @Order(2)
    public void testFindOrganizationByAccessKey() {
        Organization organization = organizationRepository.findByAccessKey("COD");
        assertThat(organization.getAccessKey()).isEqualTo("COD");
    }

    @Test
    @Order(3)
    public void testListOrganizations() {
        List<Organization> organizations = (List<Organization>) organizationRepository.findAll();
        assertThat(organizations).size().isGreaterThan(0);
    }

    @Test
    @Order(4)
    public void testUpdateOrganization() {
        Organization organization = organizationRepository.findByAccessKey("COD");
        organization.setName("Activision");

        organizationRepository.save(organization);

        Organization updatedOrganization = organizationRepository.findByAccessKey("COD");

        assertThat(updatedOrganization.getName()).isEqualTo("Activision");
    }

    @Test
    @Order(5)
    public void testDeleteOrganization() {
        Organization organization = organizationRepository.findByAccessKey("COD");

        organizationRepository.deleteById(organization.getId());

        Organization deletedOrganization = organizationRepository.findByAccessKey("COD");

        assertThat(deletedOrganization).isNull();
    }
}
