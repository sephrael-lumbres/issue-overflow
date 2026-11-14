package com.sephrael.issueoverflow.service;

import java.util.Locale;

/**
 * Email addresses are compared without case: Alice@Example.test and alice@example.test are the same mailbox, so
 * they are the same account. Addresses are stored, looked up and counted in the form this class gives them.
 */
public final class Emails {

    private Emails() {
    }

    // Trimmed and in lower case (Locale.ROOT, so the result doesn't depend on the server's language); null stays null.
    public static String normalize(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
