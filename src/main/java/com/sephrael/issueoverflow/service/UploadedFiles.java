package com.sephrael.issueoverflow.service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/**
 * Rules for the files uploaded by users.
 */
public final class UploadedFiles {

    // awsfile.file_name is VARCHAR(255)
    static final int MAX_NAME_LENGTH = 255;

    // a longer "extension" is treated as part of the name
    private static final int MAX_EXTENSION_LENGTH = 20;
    private static final String BINARY = "application/octet-stream";

    private UploadedFiles() {
    }

    // The image type the file's first bytes show (PNG, JPEG, GIF or WebP), or null.
    public static String imageType(byte[] content) {
        if(startsWith(content, 0, new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}))
            return "image/png";
        if(startsWith(content, 0, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}))
            return "image/jpeg";
        if(startsWith(content, 0, ascii("GIF87a")) || startsWith(content, 0, ascii("GIF89a")))
            return "image/gif";
        if(startsWith(content, 0, ascii("RIFF")) && startsWith(content, 8, ascii("WEBP")))
            return "image/webp";
        return null;
    }

    // The content type to store. Image types open in the browser, so one is stored only when the bytes match it;
    // anything else keeps the browser's type, or gets application/octet-stream if that is missing or claims an image.
    public static String storedContentType(String claimedType, byte[] content) {
        String imageType = imageType(content);
        if(imageType != null)
            return imageType;
        if(claimedType == null || claimedType.isBlank() || claimedType.toLowerCase(Locale.ROOT).startsWith("image/"))
            return BINARY;
        return claimedType;
    }

    private static boolean startsWith(byte[] content, int offset, byte[] prefix) {
        return content != null && content.length >= offset + prefix.length
                && Arrays.equals(content, offset, offset + prefix.length, prefix, 0, prefix.length);
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.US_ASCII);
    }

    // The name stored for an upload: the original name, cut to 255 characters when it is longer. The extension
    // (the part from the last dot) is kept, so the file still downloads with the right type.
    public static String storedName(String originalName) {
        if(originalName == null || originalName.length() <= MAX_NAME_LENGTH)
            return originalName;

        int dot = originalName.lastIndexOf('.');
        String extension = dot > 0 && originalName.length() - dot <= MAX_EXTENSION_LENGTH ? originalName.substring(dot) : "";
        int end = MAX_NAME_LENGTH - extension.length();

        // don't split a character that takes two chars (an emoji, for example)
        if(Character.isHighSurrogate(originalName.charAt(end - 1)))
            end--;

        return originalName.substring(0, end) + extension;
    }
}
