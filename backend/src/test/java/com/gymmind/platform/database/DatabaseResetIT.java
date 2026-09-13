package com.gymmind.platform.database;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseResetIT {

    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.41")
            .withDatabaseName("gymmind")
            .withUsername("root")
            .withPassword("test-root-password");

    @BeforeAll
    static void startContainer() {
        mysql.start();
    }

    @AfterAll
    static void stopContainer() {
        mysql.stop();
    }

    @Test
    void resetChangesOnlyTheExactGymmindSchema() throws SQLException {
        DatabaseNameGuard.requireAllowed("gymmind");
        try (Connection connection = DriverManager.getConnection(adminJdbcUrl(), mysql.getUsername(), mysql.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE IF NOT EXISTS gymmind_reset_sentinel");
            Set<String> before = schemasExceptGymmind(statement);

            statement.execute("DROP DATABASE IF EXISTS gymmind");
            statement.execute("CREATE DATABASE gymmind CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");

            assertThat(schemasExceptGymmind(statement)).containsExactlyInAnyOrderElementsOf(before);
            assertThat(schemas(statement)).contains("gymmind");
        }
    }

    private static String adminJdbcUrl() {
        return "jdbc:mysql://" + mysql.getHost() + ":" + mysql.getFirstMappedPort()
                + "/?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true";
    }

    private static Set<String> schemasExceptGymmind(Statement statement) throws SQLException {
        Set<String> schemas = schemas(statement);
        schemas.remove("gymmind");
        return schemas;
    }

    private static Set<String> schemas(Statement statement) throws SQLException {
        Set<String> schemas = new LinkedHashSet<>();
        try (ResultSet result = statement.executeQuery("SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA")) {
            while (result.next()) {
                schemas.add(result.getString(1));
            }
        }
        return schemas;
    }
}
