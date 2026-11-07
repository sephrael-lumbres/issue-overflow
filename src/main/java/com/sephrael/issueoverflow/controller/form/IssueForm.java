package com.sephrael.issueoverflow.controller.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * The fields that the create, edit and status forms send for an issue.
 * <p>
 * {@code project} and {@code assignedTo} are record IDs, which the controller looks up within the caller's organization.
 * {@code assignedTo} is a string because the assignee list sends a single space for "unassigned".
 * The limits match the columns: {@code description} is TEXT, the other text fields are VARCHAR(255).
 */
public record IssueForm(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 50000) String description,
        @Size(max = 255) String type,
        @Size(max = 255) String priority,
        @Size(max = 255) String status,
        Integer estimatedHours,
        @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dueDate,
        Long project,
        String assignedTo) {
}
