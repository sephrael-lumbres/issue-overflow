package com.sephrael.issueoverflow.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UploadedFilesTest {

    @Test
    void namesUpTo255CharactersAreKept() {
        String name = "n".repeat(251) + ".txt";

        assertThat(UploadedFiles.storedName(name)).isEqualTo(name);
        assertThat(UploadedFiles.storedName(null)).isNull();
    }

    @Test
    void longerNamesAreCutBeforeTheExtension() {
        assertThat(UploadedFiles.storedName("n".repeat(300) + ".tar.gz"))
                .hasSize(255)
                .isEqualTo("n".repeat(252) + ".gz");
    }

    @Test
    void aLongTailAfterTheLastDotIsNotTreatedAsAnExtension() {
        String name = "report." + "x".repeat(300);

        assertThat(UploadedFiles.storedName(name)).isEqualTo(name.substring(0, 255));
    }

    @Test
    void imagesAreRecognizedByTheirFirstBytes() {
        assertThat(UploadedFiles.imageType(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'})).isEqualTo("image/png");
        assertThat(UploadedFiles.imageType(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0})).isEqualTo("image/jpeg");
        assertThat(UploadedFiles.imageType("GIF89a...".getBytes())).isEqualTo("image/gif");
        assertThat(UploadedFiles.imageType("RIFF\0\0\0\0WEBPVP8 ".getBytes())).isEqualTo("image/webp");
        assertThat(UploadedFiles.imageType("<svg onload=alert(1)>".getBytes())).isNull();
        assertThat(UploadedFiles.imageType(new byte[0])).isNull();
        assertThat(UploadedFiles.storedContentType(null, new byte[] {1, 2, 3})).isEqualTo("application/octet-stream");
    }
}
