package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The privacy policy is a short, plain page written for this app;
 * it replaced a generated one (and the separate cookie policy) that described features the app doesn't have.
 */
class PrivacyPolicyPageTest extends WebIntegrationTestBase {

    @Test
    void anyoneCanReadThePlainPrivacyPolicy() throws Exception {
        // the template wraps its sentences over several lines; compare the text with each run of whitespace as one space
        String page = mockMvc.perform(get("/privacy"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()
                .replaceAll("\\s+", " ");

        assertThat(page)
                .contains("support@issueoverflow.app")
                .contains("JSESSIONID")
                .contains("Amazon Web Services (S3)")
                .contains("stored in the United States")
                .doesNotContainIgnoringCase("termly")
                .doesNotContain("/cookie-policy");
    }
}
