package com.sephrael.issueoverflow.controller.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The fields that the create and edit project forms send. The organization and members of a project are set by the server.
 */
public record ProjectForm(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String identifier,
        @NotBlank @Size(max = 255) String color) {
}
