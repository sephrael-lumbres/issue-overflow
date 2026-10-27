package com.sephrael.issueoverflow;

import com.sephrael.issueoverflow.entity.Role;
import com.sephrael.issueoverflow.repository.RoleRepository;
import com.sephrael.issueoverflow.support.RepositoryTestBase;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

// Flyway owns the schema and Hibernate only validates it, so a context that starts at all proves the mappings match
// the migrations. These tests check that the expected migrations ran, the audit tables exist and the roles are seeded.
class SchemaMigrationTest extends RepositoryTestBase {

    @Autowired
    private Flyway flyway;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private RoleRepository roleRepository;

    @Test
    void migrationsCreateTheSchemaIncludingAuditTables() {
        assertThat(flyway.info().applied())
                .extracting(info -> info.getVersion().getVersion(), MigrationInfo::getState)
                .containsExactly(tuple("1", MigrationState.SUCCESS), tuple("2", MigrationState.SUCCESS));

        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE'";
        List<String> tables = jdbcTemplate.queryForList(sql, String.class);

        assertThat(tables).contains("revinfo", "issue_aud", "project_aud", "users_aud", "flyway_schema_history");
    }

    @Test
    void rolesAreSeededByMigration() {
        assertThat(roleRepository.findAll())
                .extracting(Role::getName)
                .containsExactlyInAnyOrder("Project Manager", "Admin", "Developer", "Guest");
    }
}
