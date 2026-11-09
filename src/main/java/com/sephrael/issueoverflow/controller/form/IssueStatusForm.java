package com.sephrael.issueoverflow.controller.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * The field that the Mark as Resolved and Mark as Closed buttons send. Nothing else about the issue changes.
 */
public record IssueStatusForm(@NotNull @Pattern(regexp = "Resolved|Closed") String status) {
}
