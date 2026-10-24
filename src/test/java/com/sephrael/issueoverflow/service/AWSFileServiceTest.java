package com.sephrael.issueoverflow.service;

import com.sephrael.issueoverflow.configuration.S3Properties;
import com.sephrael.issueoverflow.entity.AWSFile;
import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AWSFileServiceTest extends WebIntegrationTestBase {

    @Autowired
    private S3Client s3Client;
    @Autowired
    private S3Properties s3Properties;

    @Test
    void uploadStoresTheObjectAndItsMetadata() {
        Fixture fixture = createFixture();
        byte[] content = "Stack trace from the nightly build".getBytes(StandardCharsets.UTF_8);

        AWSFile awsFile = uploadAttachment(fixture, "build-log.txt", "text/plain", content);

        assertThat(awsFile.getFileName()).isEqualTo("build-log.txt");
        assertThat(awsFile.getFileKey()).matches(UUID_PATTERN);
        assertThat(awsFile.getType()).isEqualTo("text/plain");
        assertThat(awsFile.getSize()).isEqualTo(content.length);
        assertThat(awsFile.isProfilePicture()).isFalse();
        assertThat(awsFile.getUser().getId()).isEqualTo(fixture.projectManager.getId());
        assertThat(awsFile.getProject().getId()).isEqualTo(fixture.project.getId());

        HeadObjectResponse head = s3Client.headObject(request -> request
                .bucket(s3Properties.bucket()).key(awsFile.getFileKey()));
        assertThat(head.contentType()).isEqualTo("text/plain");
        assertThat(head.contentLength()).isEqualTo(content.length);
        byte[] stored = s3Client.getObjectAsBytes(request -> request
                .bucket(s3Properties.bucket()).key(awsFile.getFileKey())).asByteArray();
        assertThat(stored).isEqualTo(content);
    }

    @Test
    void deleteFileRemovesTheObjectAndItsRecord() {
        Fixture fixture = createFixture();
        AWSFile awsFile = uploadAttachment(fixture, "screenshot.png", "image/png", new byte[] {1, 2, 3});

        awsFileService.deleteFile(awsFile.getFileKey());

        assertThat(awsFileRepository.findByFileKey(awsFile.getFileKey())).isNull();
        assertThatThrownBy(() -> s3Client.headObject(request -> request
                .bucket(s3Properties.bucket()).key(awsFile.getFileKey())))
                .isInstanceOfSatisfying(S3Exception.class,
                        exception -> assertThat(exception.statusCode()).isEqualTo(404));
    }
}
