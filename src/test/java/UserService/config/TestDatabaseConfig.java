package UserService.config;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.testcontainers.containers.PostgreSQLContainer;

public class TestDatabaseConfig {

    private static PostgreSQLContainer<?> postgresContainer;
    private static SessionFactory testSessionFactory;

    public static synchronized PostgreSQLContainer<?> getPostgresContainer() {
        if (postgresContainer == null) {
            postgresContainer = new PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName("postgres")
                    .withUsername("postgres")
                    .withPassword("postgres");


            postgresContainer.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (postgresContainer != null && postgresContainer.isRunning()) {
                    postgresContainer.stop();
                }
            }));
        }
        return postgresContainer;
    }

    public static SessionFactory createTestSessionFactory() {
        if (testSessionFactory == null) {
            PostgreSQLContainer<?> container = getPostgresContainer();

            Configuration configuration = new Configuration();
            configuration.setProperty("hibernate.connection.driver_class",
                    "org.postgresql.Driver");
            configuration.setProperty("hibernate.connection.url",
                    container.getJdbcUrl());
            configuration.setProperty("hibernate.connection.username",
                    container.getUsername());
            configuration.setProperty("hibernate.connection.password",
                    container.getPassword());
            configuration.setProperty("hibernate.dialect",
                    "org.hibernate.dialect.PostgreSQLDialect");
            configuration.setProperty("hibernate.hbm2ddl.auto", "create-drop");
            configuration.setProperty("hibernate.show_sql", "false");
            configuration.setProperty("hibernate.format_sql", "false");

            configuration.addAnnotatedClass(UserService.entity.User.class);

            testSessionFactory = configuration.buildSessionFactory();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (testSessionFactory != null) {
                    testSessionFactory.close();
                }
            }));
        }
        return testSessionFactory;
    }

    public static void clearDatabase() {
        if (testSessionFactory != null) {
            try (var session = testSessionFactory.openSession()) {
                var transaction = session.beginTransaction();
                session.createQuery("DELETE FROM User").executeUpdate();
                transaction.commit();
            }
        }
    }
}

