package com.sephrael.issueoverflow.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

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

    // What the database stores for an account token: its SHA-256 as 64 hex digits. The tokens are 256 random bits,
    // so a fast hash is enough; a copy of the users table then holds no working links.
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Every Java runtime provides SHA-256", exception);
        }
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
