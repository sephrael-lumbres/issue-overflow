package com.sephrael.issueoverflow.repository;

import com.sephrael.issueoverflow.entity.Organization;
import com.sephrael.issueoverflow.entity.Role;
import com.sephrael.issueoverflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.email = ?1")
    User findByEmail(String email);
    User findUserById(Long id);
    User findByIdAndOrganization(Long id, Organization organization);
    User findByResetPasswordToken(String token);
    User findByEmailVerificationToken(String emailVerificationToken);
    List<User> findByOrganizationAndRole(Organization organization, Role role);
    List<User> findByOrganizationOrderByRoleId(Organization organization);

    // OLD CODE to get Users by Organization and Role
    //List<User> findByRoleNameAndOrganizationName(String role, String name);
}
