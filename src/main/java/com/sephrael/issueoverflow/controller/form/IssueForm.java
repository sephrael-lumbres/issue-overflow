package com.sephrael.issueoverflow.controller.form;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * The fields that the create, edit and status forms send for an issue.
 * <p>
 * {@code project} and {@code assignedTo} are record IDs, which the controller looks up within the caller's organization.
 * {@code assignedTo} is a string because the assignee list sends a single space for "unassigned".
 */
public record IssueForm(
        String title,
        String description,
        String type,
        String priority,
        String status,
        Integer estimatedHours,
        @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dueDate,
        Long project,
        String assignedTo) {
}
