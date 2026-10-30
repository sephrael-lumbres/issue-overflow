package com.sephrael.issueoverflow.service;

import java.security.SecureRandom;
import java.util.Base64;

public final class TokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private static final String ACCESS_KEY_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int ACCESS_KEY_LENGTH = 10;

    private TokenGenerator() {
    }

    // 32 random bytes as URL-safe Base64 without padding: 43 characters
    public static String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // An organization's join key: 10 letters and digits (about 59.5 bits), short enough to type
    public static String newAccessKey() {
        StringBuilder key = new StringBuilder(ACCESS_KEY_LENGTH);
        for (int i = 0; i < ACCESS_KEY_LENGTH; i++) {
            key.append(ACCESS_KEY_ALPHABET.charAt(RANDOM.nextInt(ACCESS_KEY_ALPHABET.length())));
        }

        return key.toString();
    }
}
