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
}
