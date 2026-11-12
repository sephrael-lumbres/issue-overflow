package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pages that were never finished (theme demo pages for planned features) are gone, routes included, and so is the
 * separate cookie policy, which the privacy policy now covers.
 */
class RemovedPagesTest extends WebIntegrationTestBase {

    @Test
    void removedPagesAreNotFound() throws Exception {
        MockHttpSession session = login(createFixture().projectManager.getEmail());
        List<String> removedPages = List.of("/multi-tenant-add-users.html", "/user-management-add-user.html", "/cookie-policy");

        for (String path : removedPages) {
            mockMvc.perform(get(path).session(session)).andExpect(status().isNotFound());
        }
    }
}
