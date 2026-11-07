package com.sephrael.issueoverflow.controller.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The fields that the registration form sends. The role, organization and account state of a new user are set by the server.
 * The password is checked with {@link Passwords#errorMessage(String)}, since part of its limit is in bytes.
 */
public record RegistrationForm(
        @NotBlank @Size(max = 255) String firstName,
        @NotBlank @Size(max = 255) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        String password) {
}
