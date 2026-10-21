package com.sephrael.issueoverflow.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenGeneratorTest {

    @Test
    void tokensAreUrlSafeAndUnique() {
        String first = TokenGenerator.newToken();
        String second = TokenGenerator.newToken();

        assertThat(first).matches("[A-Za-z0-9_-]{43}");
        assertThat(second).matches("[A-Za-z0-9_-]{43}");
        assertThat(first).isNotEqualTo(second);
    }
}
