package com.sephrael.issueoverflow.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContactSubjectNameTest {

    @Test
    void controlCharactersBecomeSpacesAndLongNamesAreCut() {
        assertThat(HelpController.subjectName(" Ada\tLovelace\r\n")).isEqualTo("Ada Lovelace");
        assertThat(HelpController.subjectName("a".repeat(150))).hasSize(HelpController.MAX_SUBJECT_NAME_LENGTH);
        // cut by code point, so a character outside the BMP is never split in half
        assertThat(HelpController.subjectName("😀".repeat(150)).codePoints().count())
                .isEqualTo((long) HelpController.MAX_SUBJECT_NAME_LENGTH);
    }
}
