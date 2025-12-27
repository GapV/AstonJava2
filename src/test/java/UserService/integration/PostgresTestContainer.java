package UserService.integration;

import UserService.config.TestDatabaseConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ExtendWith({PostgreSQLExtension.class})
public abstract class PostgresTestContainer {

    @BeforeEach
    void setUp() {
        TestDatabaseConfig.clearDatabase();
    }

    @AfterEach
    void tearDown() {
        TestDatabaseConfig.clearDatabase();
    }

    protected String getJdbcUrl() {
        return TestDatabaseConfig.getPostgresContainer().getJdbcUrl();
    }

    protected String getUsername() {
        return TestDatabaseConfig.getPostgresContainer().getUsername();
    }

    protected String getPassword() {
        return TestDatabaseConfig.getPostgresContainer().getPassword();
    }
}

class PostgreSQLExtension implements org.junit.jupiter.api.extension.BeforeAllCallback {

    @Override
    public void beforeAll(org.junit.jupiter.api.extension.ExtensionContext context) {
        TestDatabaseConfig.getPostgresContainer();
        TestDatabaseConfig.createTestSessionFactory();
    }
}