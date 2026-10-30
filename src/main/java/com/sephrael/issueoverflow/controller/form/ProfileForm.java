package com.sephrael.issueoverflow.controller.form;

/**
 * The fields that the account profile form sends.
 */
public record ProfileForm(String firstName, String lastName, String email) {
}
