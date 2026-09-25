package com.adrianperezcobo.dummycommerce.users.shared.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest(
        properties = "spring.jpa.hibernate.ddl-auto=validate"
)
@AutoConfigureTestDatabase(replace = NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers
class FlywayMigrationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreateUsersTables() {
        assertThat(tableExists("users"))
                .isEqualTo(1);

        assertThat(tableExists("refresh_tokens"))
                .isEqualTo(1);
    }

    @Test
    void shouldApplyFlywayMigrations() {
        Integer appliedMigrations =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM flyway_schema_history
                        WHERE success = TRUE
                        """,
                        Integer.class
                );

        assertThat(appliedMigrations)
                .isNotNull()
                .isGreaterThanOrEqualTo(2);
    }

    private Integer tableExists(String tableName) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name = ?
                """,
                Integer.class,
                tableName
        );
    }
}