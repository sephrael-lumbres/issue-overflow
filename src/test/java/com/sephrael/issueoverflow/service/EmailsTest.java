package com.sephrael.issueoverflow.service;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class EmailsTest {

    // Locale.ROOT keeps an 'I' an 'i' even where the default locale's rules differ (Turkish lowers I to a dotless i).
    @Test
    void addressesAreTrimmedAndLowerCasedWhateverTheDefaultLocale() {
        Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));

            assertThat(Emails.normalize("  Ivan.ILYIN@Example.TEST ")).isEqualTo("ivan.ilyin@example.test");
            assertThat(Emails.normalize(null)).isNull();
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }
}
