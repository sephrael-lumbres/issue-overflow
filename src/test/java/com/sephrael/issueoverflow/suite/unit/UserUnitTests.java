package com.sephrael.issueoverflow.suite.unit;

import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.repository.UserRepository;
import com.sephrael.issueoverflow.support.RepositoryTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class UserUnitTests extends RepositoryTestBase {

    private static final String EMAIL = "user1@example.com";

    @Autowired
    private UserRepository userRepository;

    private User savedUser() {
        return userRepository.save(newUser(EMAIL, "Ravi", "Kumar"));
    }

    @Test
    void testCreateUser() {
        User user = savedUser();
        flushAndClear();

        User existingUser = entityManager.find(User.class, user.getId());

        assert existingUser != null;
        assertThat(existingUser.getEmail()).isEqualTo(EMAIL);
        assertThat(existingUser.getId()).isEqualTo(user.getId());
    }

    @Test
    void testFindUserByEmail() {
        savedUser();
        flushAndClear();

        assertThat(userRepository.findByEmail(EMAIL).getFirstName()).isEqualTo("Ravi");
    }

    @Test
    void testListUsers() {
        User user = savedUser();
        flushAndClear();

        assertThat(userRepository.findAll())
                .extracting(User::getId)
                .contains(user.getId());
    }

    @Test
    void testUpdateUser() {
        savedUser();
        flushAndClear();

        User user = userRepository.findByEmail(EMAIL);
        user.setPassword("password");
        userRepository.save(user);
        flushAndClear();

        assertThat(userRepository.findByEmail(EMAIL).getPassword()).isEqualTo("password");
    }

    @Test
    void testDeleteUser() {
        User user = savedUser();
        flushAndClear();

        userRepository.deleteById(user.getId());
        flushAndClear();

        assertThat(userRepository.findByEmail(EMAIL)).isNull();
    }
}
