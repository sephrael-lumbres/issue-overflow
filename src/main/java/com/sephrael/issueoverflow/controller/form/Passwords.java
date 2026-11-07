package com.sephrael.issueoverflow.controller.form;

import java.nio.charset.StandardCharsets;

/**
 * The rule for new passwords: 8 to 64 characters. BCrypt reads at most 72 bytes of a password, and Spring Security's
 * encoder throws for a longer one (a 500 before this check). A password whose characters take more than 72 bytes in
 * UTF-8 (many accented letters or emoji) is therefore refused with its own message.
 */
public final class Passwords {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 64;
    public static final int MAX_BYTES = 72;

    public static final String REQUIREMENT = "Passwords must be between 8 and 64 characters.";
    public static final String TOO_MANY_BYTES = "That password is too long. Use fewer accented letters or emoji.";

    private Passwords() {
    }

    // The message to show for a new password, or null when the password is acceptable.
    public static String errorMessage(String password) {
        if(password == null || password.isBlank())
            return REQUIREMENT;

        int length = password.codePointCount(0, password.length());
        if(length < MIN_LENGTH || length > MAX_LENGTH)
            return REQUIREMENT;

        if(password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES)
            return TOO_MANY_BYTES;

        return null;
    }
}
