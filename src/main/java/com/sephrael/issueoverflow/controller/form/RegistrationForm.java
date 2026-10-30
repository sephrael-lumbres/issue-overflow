package com.sephrael.issueoverflow.controller.form;

/**
 * The fields that the registration form sends. The role, organization and account state of a new user are set by the server.
 */
public record RegistrationForm(String firstName, String lastName, String email, String password) {
}
