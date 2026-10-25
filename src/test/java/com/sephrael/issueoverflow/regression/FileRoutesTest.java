package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.configuration.S3Properties;
import com.sephrael.issueoverflow.entity.AWSFile;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FileRoutesTest extends WebIntegrationTestBase {

    @Autowired
    private S3Properties s3Properties;

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

    @Test
    void attachmentRedirectsToAPresignedDownloadWithinTheOrganization() throws Exception {
        Fixture fixture = createFixture();
        byte[] content = "Steps to reproduce: open the dashboard twice.".getBytes(StandardCharsets.UTF_8);
        AWSFile awsFile = uploadAttachment(fixture, "steps.txt", "text/plain", content);
        User developer = createUser(fixture.organization, "Developer");
        MockHttpSession session = login(developer.getEmail());

        MvcResult result = mockMvc.perform(get("/storage/files/" + awsFile.getFileKey()).session(session))
                .andExpect(status().isFound())
                .andReturn();

        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(location)
                .contains("/" + s3Properties.bucket() + "/" + awsFile.getFileKey() + "?")
                .contains("X-Amz-Signature=")
                .contains("response-content-disposition=attachment");
        assertThat(result.getResponse().getHeader(HttpHeaders.CACHE_CONTROL))
                .contains("max-age=300")
                .contains("private");

        HttpResponse<byte[]> download = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(location)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertThat(download.statusCode()).isEqualTo(200);
        assertThat(download.body()).isEqualTo(content);
    }

    @Test
    void attachmentIsNotFoundOutsideItsOrganization() throws Exception {
        Fixture fixture = createFixture();
        AWSFile awsFile = uploadAttachment(fixture, "steps.txt", "text/plain", new byte[] {1, 2, 3});
        User outsider = createUser(createOrganization(), "Project Manager");
        MockHttpSession session = login(outsider.getEmail());

        mockMvc.perform(get("/storage/files/" + awsFile.getFileKey()).session(session))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/storage/files/" + UUID.randomUUID()).session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void profilePictureIsShownInlineWithinTheOrganizationOnly() throws Exception {
        Fixture fixture = createFixture();
        User owner = fixture.projectManager;
        MockHttpSession ownerSession = login(owner.getEmail());
        MockMultipartFile picture = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[] {(byte) 0x89, 'P', 'N', 'G'});

        mockMvc.perform(multipart("/storage/upload/" + owner.getId() + "/true")
                        .file(picture).session(ownerSession).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account/profile/" + owner.getId()));

        User reloaded = userRepository.findByEmail(owner.getEmail());
        assertThat(reloaded.hasProfilePicture()).isTrue();
        String key = reloaded.getProfilePictureKey();
        assertThat(key).matches(UUID_PATTERN);

        String dashboard = mockMvc.perform(get("/dashboard").session(ownerSession))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(dashboard).contains("/storage/files/" + key);

        User colleague = createUser(fixture.organization, "Guest");
        MvcResult colleagueResult = mockMvc.perform(get("/storage/files/" + key).session(login(colleague.getEmail())))
                .andExpect(status().isFound())
                .andReturn();
        assertThat(colleagueResult.getResponse().getHeader(HttpHeaders.LOCATION))
                .contains("response-content-disposition=inline");

        User outsider = createUser(createOrganization(), "Guest");
        mockMvc.perform(get("/storage/files/" + key).session(login(outsider.getEmail())))
                .andExpect(status().isNotFound());
    }
}
