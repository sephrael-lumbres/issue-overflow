package com.sephrael.issueoverflow.controller.form;

/**
 * The fields that the comment forms send. The author and issue of a comment are set by the server.
 */
public record CommentForm(String message) {
}
