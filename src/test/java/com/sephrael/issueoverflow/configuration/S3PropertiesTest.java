package com.sephrael.issueoverflow.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class S3PropertiesTest {

    @Test
    void toStringLeavesOutTheCredentials() {
        S3Properties properties = new S3Properties(
                "files-bucket", "us-west-2", "test-access-key-id", "test-secret-access-key", null);

        assertThat(properties.toString())
                .contains("files-bucket", "us-west-2")
                .doesNotContain("test-access-key-id", "test-secret-access-key");
    }
}
