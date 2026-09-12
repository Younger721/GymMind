package com.gymmind.platform.bootstrap;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class DatabaseInitializationLock {
    private static final int LOCK_TIMEOUT_SECONDS = 30;

    private final DataSource dataSource;
    private final TransactionTemplate transactionTemplate;

    public DatabaseInitializationLock(DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.dataSource = dataSource;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void execute(String lockName, Runnable initialization) {
        try (Connection connection = dataSource.getConnection()) {
            acquire(connection, lockName);
            try {
                transactionTemplate.executeWithoutResult(status -> initialization.run());
            } finally {
                release(connection, lockName);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Database initialization lock failed: " + lockName, exception);
        }
    }

    private static void acquire(Connection connection, String lockName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select get_lock(?, ?)")) {
            statement.setString(1, lockName);
            statement.setInt(2, LOCK_TIMEOUT_SECONDS);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || result.getInt(1) != 1) {
                    throw new IllegalStateException("Timed out acquiring database initialization lock: " + lockName);
                }
            }
        }
    }

    private static void release(Connection connection, String lockName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select release_lock(?)")) {
            statement.setString(1, lockName);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || result.getInt(1) != 1) {
                    throw new IllegalStateException("Database initialization lock was not held: " + lockName);
                }
            }
        }
    }
}
