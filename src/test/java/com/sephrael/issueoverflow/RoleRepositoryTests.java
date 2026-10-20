package com.sephrael.issueoverflow;

import com.sephrael.issueoverflow.entity.Role;
import com.sephrael.issueoverflow.repository.RoleRepository;
import com.sephrael.issueoverflow.support.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(value = false)
public class RoleRepositoryTests extends PostgresTestBase {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    public void testCreateRoles() {
        Role projectManager = new Role();
        Role admin = new Role();
        Role developer = new Role();
        Role guest = new Role();

        projectManager.setName("Project Manager");
        admin.setName("Admin");
        developer.setName("Developer");
        guest.setName("Guest");

        long rolesBefore = roleRepository.count();

        roleRepository.saveAll(List.of(projectManager, admin, developer, guest));

        assertThat(roleRepository.count()).isEqualTo(rolesBefore + 4);
    }
}
