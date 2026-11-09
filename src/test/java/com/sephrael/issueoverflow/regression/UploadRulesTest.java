package com.sephrael.issueoverflow.regression;

import com.sephrael.issueoverflow.entity.AWSFile;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The upload route takes profile pictures only, and a file's stored type follows its bytes when it claims to be an image.
 */
class UploadRulesTest extends WebIntegrationTestBase {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13};

    // "/storage/upload/{id}/false" stored a file that was neither a profile picture nor an attachment, and deleted the user's profile picture.
    @Test
    void theUploadRouteNoLongerTakesOtherFiles() throws Exception {
        User user = createFixture().projectManager;
        inTransaction(() -> {
            User owner = userRepository.findUserById(user.getId());
            owner.setHasProfilePicture(true);
            awsFileService.uploadFile(new MockMultipartFile("file", "avatar.png", "image/png", PNG), owner, null, true);
        });
        String pictureKey = userRepository.findUserById(user.getId()).getProfilePictureKey();
        MockHttpSession session = login(user.getEmail());

        mockMvc.perform(multipart("/storage/upload/" + user.getId() + "/false")
                        .file(new MockMultipartFile("file", "notes.txt", "text/plain", "notes".getBytes(StandardCharsets.UTF_8)))
                        .session(session).with(csrf()))
                .andExpect(status().is4xxClientError());

        User reloadedUser = userRepository.findUserById(user.getId());
        assertThat(reloadedUser.hasProfilePicture()).isTrue();
        assertThat(reloadedUser.getProfilePictureKey()).isEqualTo(pictureKey);
        assertThat(awsFileRepository.findByFileKey(pictureKey)).isNotNull();
    }

    // An attachment that says it is a PNG but is HTML would have opened in the browser.
    @Test
    void anAttachmentThatOnlyClaimsToBeAnImageIsStoredAsBinary() {
        Fixture fixture = createFixture();

        AWSFile awsFile = uploadAttachment(fixture, "photo.png", "image/png",
                "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8));

        assertThat(awsFile.getType()).isEqualTo("application/octet-stream");
    }

    @Test
    void realImagesAndOtherFilesKeepTheirTypes() {
        Fixture image = createFixture();
        Fixture document = createFixture();

        assertThat(uploadAttachment(image, "screenshot.png", "image/png", PNG).getType()).isEqualTo("image/png");
        assertThat(uploadAttachment(document, "report.pdf", "application/pdf", "%PDF-1.7".getBytes(StandardCharsets.US_ASCII)).getType())
                .isEqualTo("application/pdf");
    }

    @Test
    void aProfilePictureMustBeAnImage() throws Exception {
        User user = createFixture().projectManager;
        MockHttpSession session = login(user.getEmail());

        mockMvc.perform(multipart("/storage/upload/" + user.getId())
                        .file(new MockMultipartFile("file", "avatar.png", "image/png", "not an image".getBytes(StandardCharsets.UTF_8)))
                        .session(session).with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(userRepository.findUserById(user.getId()).hasProfilePicture()).isFalse();
    }
}
