package com.sephrael.issueoverflow.suite.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sephrael.issueoverflow.entity.*;
import com.sephrael.issueoverflow.repository.*;
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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(value = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserUnitTests extends PostgresTestBase {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private UserRepository userRepository;

    @Test
    @Order(1)
    public void testCreateUser() {
        User user = new User();
        user.setId(user.getId());
        user.setEmail("user1@example.com");
        user.setPassword("test-password-1");
        user.setFirstName("Ravi");
        user.setLastName("Kumar");

        User savedUser = userRepository.save(user);

        User existingUser = entityManager.find(User.class, savedUser.getId());

        assertThat(user.getEmail()).isEqualTo(existingUser.getEmail());
        assertThat(user.getId()).isEqualTo(existingUser.getId());
    }

    @Test
    @Order(2)
    public void testFindUserByEmail() {
        User user = userRepository.findByEmail("user1@example.com");
        assertThat(user.getEmail()).isEqualTo("user1@example.com");
    }

    @Test
    @Order(3)
    public void testListUsers() {
        List<User> users = (List<User>) userRepository.findAll();
        assertThat(users).size().isGreaterThan(0);
    }

    @Test
    @Order(4)
    public void testUpdateUser() {
        User user = userRepository.findByEmail("user1@example.com");
        user.setPassword("password");

        userRepository.save(user);

        User updatedUser = userRepository.findByEmail("user1@example.com");

        assertThat(updatedUser.getPassword()).isEqualTo("password");
    }

    @Test
    @Order(5)
    public void testDeleteUser() {
        User user = userRepository.findByEmail("user1@example.com");

        userRepository.deleteById(user.getId());

        User deletedUser = userRepository.findByEmail("user1@example.com");

        assertThat(deletedUser).isNull();
    }
}
