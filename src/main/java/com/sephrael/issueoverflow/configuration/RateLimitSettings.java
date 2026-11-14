package com.sephrael.issueoverflow.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rate limit settings, bound from {@code app.rate-limit.*}.
 * <p>
 * {@code enabled}: when false, no limit applies; the test profile sets it to false, because the tests post the same
 * forms many times from one address.
 * {@code clientIpHeader}: the header that holds the client's IP address, or blank to use the connection's address.
 * Name only a header that the hosting proxy overwrites on every request, since a client can send any other.
 * The prod profile sets X-Real-IP.
 */
@ConfigurationProperties("app.rate-limit")
public record RateLimitSettings(boolean enabled, String clientIpHeader) {
}
