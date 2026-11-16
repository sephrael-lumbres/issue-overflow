package com.sephrael.issueoverflow.configuration;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class ResendSettingsTest {

    @Test
    void theApiKeyIsNeverPrinted() {
        ResendSettings settings = new ResendSettings("re_test_secret_value", URI.create("https://api.resend.test"));

        assertThat(settings.toString())
                .contains("https://api.resend.test")
                .doesNotContain("re_test_secret_value");
    }
}
