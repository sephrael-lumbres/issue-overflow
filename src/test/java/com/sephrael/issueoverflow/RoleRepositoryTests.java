package com.sephrael.issueoverflow;

import com.sephrael.issueoverflow.repository.RoleRepository;
import com.sephrael.issueoverflow.support.RepositoryTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Uses role names the application never seeds, so the test does not depend on which roles already exist.
class RoleRepositoryTests extends RepositoryTestBase {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void testCreateRoles() {
        long rolesBefore = roleRepository.count();

        roleRepository.saveAll(List.of(
                newRole("Release Manager"),
                newRole("Reviewer"),
                newRole("Triager"),
                newRole("Observer")
        ));
        flushAndClear();

        assertThat(roleRepository.count()).isEqualTo(rolesBefore + 4);
        assertThat(roleRepository.findByName("Reviewer").getName()).isEqualTo("Reviewer");
    }
}
