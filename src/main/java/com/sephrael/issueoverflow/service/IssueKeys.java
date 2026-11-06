package com.sephrael.issueoverflow.service;

import com.sephrael.issueoverflow.entity.Project;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Issue keys ("IST-7"): the project's identifier and the next number from the project's counter (issue_key_sequence).
 */
@Component
public class IssueKeys {

    // Creates the project's counter at 1, or adds 1 to it, and returns the new number.
    // It is one statement, so concurrent requests get different numbers.
    // The counter never goes down, so a deleted issue's number is not reused.
    private static final String NEXT_NUMBER = """
            INSERT INTO issue_key_sequence (project_id, issue_key_counter)
            VALUES (:projectId, 1)
            ON CONFLICT (project_id) DO UPDATE
                SET issue_key_counter = issue_key_sequence.issue_key_counter + 1
            RETURNING issue_key_counter
            """;

    private final JdbcClient jdbcClient;

    public IssueKeys(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    public String nextKey(Project project) {
        Integer number = jdbcClient.sql(NEXT_NUMBER)
                .param("projectId", project.getId())
                .query(Integer.class)
                .single();

        return project.getIdentifier() + "-" + number;
    }
}
