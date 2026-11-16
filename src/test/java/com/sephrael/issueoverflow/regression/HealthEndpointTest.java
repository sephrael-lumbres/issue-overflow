package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The deployment's health check is public and says only UP or DOWN; no other actuator endpoint is served.
 */
class HealthEndpointTest extends WebIntegrationTestBase {

    @Test
    void anyoneCanReadTheHealthWithoutDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    // signed in, so the security rules let the requests through: nothing answers them
    @Test
    void noOtherActuatorEndpointIsServed() throws Exception {
        User user = createUser(createOrganization(), "Developer");
        MockHttpSession session = login(user.getEmail());

        for(String path : new String[] {"/actuator", "/actuator/env", "/actuator/info", "/actuator/metrics", "/actuator/health/db"}) {
            mockMvc.perform(get(path).session(session)).andExpect(status().isNotFound());
        }
    }
}
