package com.sephrael.issueoverflow.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

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

    @Test
    void accessKeysAreTenLettersOrDigits() {
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            keys.add(TokenGenerator.newAccessKey());
        }

        assertThat(keys).hasSize(200).allMatch(key -> key.matches("[0-9A-Za-z]{10}"));
    }
}
