package com.sephrael.issueoverflow.controller.form;

/**
 * The fields that the create and edit project forms send. The organization and members of a project are set by the server.
 */
public record ProjectForm(String name, String identifier, String color) {
}
