package com.sephrael.issueoverflow.controller.form;

import com.sephrael.issueoverflow.service.Emails;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The fields that the account profile form sends.
 */
public record ProfileForm(
        @NotBlank @Size(max = 255) String firstName,
        @NotBlank @Size(max = 255) String lastName,
        @NotBlank @Email @Size(max = 255) String email) {

    // addresses are compared without case, so a changed address is kept in lower case
    public ProfileForm {
        email = Emails.normalize(email);
    }
}
