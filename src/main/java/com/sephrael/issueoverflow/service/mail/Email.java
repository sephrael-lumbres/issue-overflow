package com.sephrael.issueoverflow.service.mail;

import java.util.List;

/**
 * One HTML email, built in the request and sent later by {@link EmailSender}.
 * It holds only strings, so nothing in it needs the request's database session.
 */
public record Email(String from, String fromName, List<String> to, String subject, String html) {

    public Email {
        to = List.copyOf(to);
    }
}
