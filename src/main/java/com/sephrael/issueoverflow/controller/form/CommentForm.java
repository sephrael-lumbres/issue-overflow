package com.sephrael.issueoverflow.controller.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The fields that the comment forms send. The author and issue of a comment are set by the server.
 */
public record CommentForm(@NotBlank @Size(max = 20000) String message) {
}
