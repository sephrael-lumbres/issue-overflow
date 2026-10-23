package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FileRoutesTest extends WebIntegrationTestBase {

    // The download and profile-picture routes streamed S3 objects through the app and were no longer linked from any template.
    @Test
    void legacyDownloadRoutesAreGone() throws Exception {
        Fixture fixture = createFixture();
        MockHttpSession session = login(fixture.projectManager.getEmail());

        mockMvc.perform(get("/storage/download/any-key").session(session))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/storage/profile-picture/" + fixture.projectManager.getId()).session(session))
                .andExpect(status().isNotFound());
    }
}
