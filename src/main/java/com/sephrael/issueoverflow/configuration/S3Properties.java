package com.sephrael.issueoverflow.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.net.URI;
import java.time.Duration;

/**
 * S3 settings, bound from {@code app.s3.*}.
 * <p>
 * {@code endpoint} is only set in tests, where it points at an S3-compatible
 * mock. When it is set, clients use path-style URLs
 * ({@code http://host:port/bucket/key}).
 * <p>
 * {@code presignedUrlTtl} is how long a pre-signed download URL stays valid.
 */
@ConfigurationProperties("app.s3")
public record S3Properties(
        String bucket,
        String region,
        String accessKeyId,
        String secretAccessKey,
        URI endpoint,
        Duration presignedUrlTtl) {

    public S3Properties {
        Assert.hasText(bucket, "app.s3.bucket must be set");
        Assert.hasText(region, "app.s3.region must be set");
        Assert.hasText(accessKeyId, "app.s3.access-key-id must be set");
        Assert.hasText(secretAccessKey, "app.s3.secret-access-key must be set");
        Assert.notNull(presignedUrlTtl, "app.s3.presigned-url-ttl must be set");
    }

    // Leaves out the credentials, so logging this record can't leak them.
    @Override
    public String toString() {
        return "S3Properties[bucket=" + bucket + ", region=" + region + ", endpoint=" + endpoint + ", presignedUrlTtl=" + presignedUrlTtl + "]";
    }
}
