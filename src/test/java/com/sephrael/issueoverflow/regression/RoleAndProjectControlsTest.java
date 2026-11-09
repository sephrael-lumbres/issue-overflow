package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoleAndProjectControlsTest extends WebIntegrationTestBase {

    // The user edit form posts a role id. An id that isn't a role was a NullPointerException (500).
    @Test
    void anUnknownRoleIdIsNotFound() throws Exception {
        Fixture fixture = createFixture();
        User developer = createUser(fixture.organization, "Developer");
        MockHttpSession session = login(fixture.projectManager.getEmail());

        mockMvc.perform(post("/users/update/" + developer.getId()).session(session).with(csrf()).param("role", "999999999"))
                .andExpect(status().isNotFound());

        assertThat(userRepository.findUserById(developer.getId()).getRole().getName()).isEqualTo("Developer");
    }

    // Editing and deleting projects is for Project Managers (the security rules); the list showed both buttons to Admins too.
    @Test
    void onlyProjectManagersSeeTheProjectEditAndDeleteButtons() throws Exception {
        Fixture fixture = createFixture();
        User admin = createUser(fixture.organization, "Admin");
        fixture.project.addUser(admin);
        projectRepository.save(fixture.project);
        String editButton = "data-bs-target=\"#editProjectModal" + fixture.project.getId() + "\"";
        String deleteForm = "action=\"/projects/delete/" + fixture.project.getId() + "\"";

        assertThat(projectsList(login(fixture.projectManager.getEmail()))).contains(editButton).contains(deleteForm);
        assertThat(projectsList(login(admin.getEmail()))).doesNotContain(editButton).doesNotContain(deleteForm);
    }

    private String projectsList(MockHttpSession session) throws Exception {
        return mockMvc.perform(get("/projects/all").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
