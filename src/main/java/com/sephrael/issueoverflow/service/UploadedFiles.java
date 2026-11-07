package com.sephrael.issueoverflow.service;

/**
 * Rules for the files uploaded by users.
 */
public final class UploadedFiles {

    // awsfile.file_name is VARCHAR(255)
    static final int MAX_NAME_LENGTH = 255;

    // a longer "extension" is treated as part of the name
    private static final int MAX_EXTENSION_LENGTH = 20;

    private UploadedFiles() {
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
