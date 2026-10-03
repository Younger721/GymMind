package com.gymmind.platform.bootstrap;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

/** Creates the local-MySQL queue table when the persistent queue is enabled. */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "gymmind.job-queue", havingValue = "mysql")
@Order(0)
public class AsyncJobSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public AsyncJobSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS async_job (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    type VARCHAR(64) NOT NULL,
                    tenant_id BIGINT NOT NULL,
                    status VARCHAR(16) NOT NULL,
                    attempts INT NOT NULL DEFAULT 0,
                    next_attempt_at TIMESTAMP(6) NOT NULL,
                    lease_until TIMESTAMP(6) NULL,
                    last_error VARCHAR(1000) NULL,
                    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                    INDEX idx_async_job_claim(status, next_attempt_at, id),
                    INDEX idx_async_job_tenant(tenant_id)
                )
                """);
    }
}
