package com.openelements.crm;

import com.openelements.spring.base.services.user.SystemUser;
import com.openelements.spring.base.services.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared test base for Spring Boot tests that touch the database. Starts a
 * single {@code postgres:17-alpine} container once per JVM, registers it as
 * the application {@link javax.sql.DataSource} via {@link ServiceConnection},
 * and truncates all application tables between tests for per-method isolation.
 *
 * <p>The container is reused across runs when {@code testcontainers.reuse.enable=true}
 * is set in {@code ~/.testcontainers.properties} (local dev). CI runs ignore
 * the reuse hint, so each build gets a fresh container.
 *
 * <p>Adding a new table in a Flyway migration requires extending {@link #TABLES_TO_TRUNCATE}.
 * This is intentional — the explicit list keeps the truncate fast and predictable.
 *
 * <p>The {@code @Testcontainers} JUnit extension is intentionally not used —
 * it discovers {@code @Container} fields and does nothing for a manually
 * started container. The {@code static { POSTGRES.start(); }} block is what
 * actually brings the container up before any subclass context loads.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractDbTest {

    /**
     * Tables truncated between tests. Order is irrelevant because
     * {@code CASCADE} drops dependent rows.
     */
    private static final String TABLES_TO_TRUNCATE = String.join(", ",
        "api_keys",
        "audit_log",
        "comments",
        "company_comments",
        "company_tags",
        "contact_comments",
        "contact_social_links",
        "contact_tags",
        "contacts",
        "companies",
        "settings",
        "task_comments",
        "task_tags",
        "tasks",
        "tags",
        "users",
        "webhooks"
    );

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:17-alpine")
            .withReuse(true);

    static {
        POSTGRES.start();
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void truncateAllTables() {
        // RESTART IDENTITY keeps sequence-backed columns deterministic; CASCADE
        // drops dependent rows so order of tables is irrelevant.
        jdbcTemplate.execute("TRUNCATE TABLE " + TABLES_TO_TRUNCATE + " RESTART IDENTITY CASCADE");
    }

    /**
     * (Re-)inserts the {@link SystemUser} row needed by audit-log / comment
     * tests that reference {@code SystemUser.ID} as a foreign key. Idempotent:
     * subclasses call this from their own {@code @BeforeEach} when needed.
     * The {@code @AfterEach} truncate wipes {@code users}, so the seed must
     * be re-applied per test method.
     */
    protected void seedSystemUser() {
        if (userRepository.findBySub(SystemUser.SUB).isEmpty()) {
            jdbcTemplate.update(
                "INSERT INTO users (id, sub, name, created_at, updated_at) "
                    + "VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                SystemUser.ID, SystemUser.SUB, SystemUser.NAME);
        }
    }
}
