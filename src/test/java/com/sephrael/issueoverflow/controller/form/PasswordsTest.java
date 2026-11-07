package com.sephrael.issueoverflow.controller.form;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordsTest {

    @Test
    void passwordsOf8To64CharactersAreAccepted() {
        assertThat(Passwords.errorMessage("a".repeat(8))).isNull();
        assertThat(Passwords.errorMessage("a".repeat(64))).isNull();
        // 36 characters of 2 bytes each: 72 bytes
        assertThat(Passwords.errorMessage("é".repeat(36))).isNull();
    }

    @Test
    void shortLongBlankAndOver72BytePasswordsAreRejected() {
        assertThat(Passwords.errorMessage(null)).isEqualTo(Passwords.REQUIREMENT);
        assertThat(Passwords.errorMessage(" ".repeat(8))).isEqualTo(Passwords.REQUIREMENT);
        assertThat(Passwords.errorMessage("a".repeat(7))).isEqualTo(Passwords.REQUIREMENT);
        assertThat(Passwords.errorMessage("a".repeat(65))).isEqualTo(Passwords.REQUIREMENT);
        // 37 characters of 2 bytes each: 74 bytes
        assertThat(Passwords.errorMessage("é".repeat(37))).isEqualTo(Passwords.TOO_MANY_BYTES);
    }
}
