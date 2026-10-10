package com.gymmind.platform.bootstrap;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/** Keeps the development database compatible with platform-scoped documents. */
@Component
@Profile("dev")
public class KnowledgeDocumentSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public KnowledgeDocumentSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        jdbc.execute("ALTER TABLE knowledge_document MODIFY COLUMN tenant_id BIGINT NULL");
        addColumnIfMissing("ALTER TABLE knowledge_document ADD COLUMN chunk_strategy VARCHAR(32) NULL");
        addColumnIfMissing("ALTER TABLE knowledge_document ADD COLUMN chunk_strategy_version INT NULL");
        addColumnIfMissing("ALTER TABLE knowledge_document ADD COLUMN knowledge_key VARCHAR(128) NULL");
        addColumnIfMissing("ALTER TABLE knowledge_document ADD COLUMN knowledge_version INT NOT NULL DEFAULT 1");
        addColumnIfMissing("ALTER TABLE knowledge_document ADD COLUMN knowledge_type VARCHAR(32) NULL");
        addColumnIfMissing("ALTER TABLE knowledge_document ADD COLUMN applicable_product VARCHAR(128) NULL");
        addColumnIfMissing("ALTER TABLE knowledge_document ADD COLUMN price_amount DECIMAL(12,2) NULL");
        addColumnIfMissing("ALTER TABLE knowledge_document ADD COLUMN discount_type VARCHAR(32) NULL");
        addColumnIfMissing("ALTER TABLE knowledge_document ADD COLUMN discount_value DECIMAL(12,4) NULL");
        addColumnIfMissing("ALTER TABLE knowledge_document ADD COLUMN effective_from DATETIME NULL");
        addColumnIfMissing("ALTER TABLE knowledge_document ADD COLUMN effective_to DATETIME NULL");
        addColumnIfMissing("ALTER TABLE knowledge_document ADD COLUMN promotion_conditions VARCHAR(1000) NULL");
        addColumnIfMissing("ALTER TABLE knowledge_document ADD COLUMN promotion_priority INT NULL");
        addColumnIfMissing("ALTER TABLE knowledge_document ADD COLUMN chunk_artifact_key VARCHAR(512) NULL");
        jdbc.execute("CREATE TABLE IF NOT EXISTS knowledge_document_audience_role (document_id BIGINT NOT NULL, role_code VARCHAR(32) NOT NULL, PRIMARY KEY (document_id, role_code))");
        backfillAudienceRoles();
    }

    private void addColumnIfMissing(String sql) {
        try {
            jdbc.execute(sql);
        } catch (DataAccessException ignored) {
            // Hibernate ddl-auto=update normally creates these columns. The
            // guarded statement keeps startup compatible with older databases.
        }
    }

    private void backfillAudienceRoles() {
        try {
            jdbc.execute("INSERT IGNORE INTO knowledge_document_audience_role (document_id, role_code) SELECT id, 'PLATFORM_ADMIN' FROM knowledge_document WHERE tenant_id IS NULL");
            jdbc.execute("INSERT IGNORE INTO knowledge_document_audience_role (document_id, role_code) SELECT id, 'GYM_ADMIN' FROM knowledge_document WHERE tenant_id IS NULL OR tenant_id IS NOT NULL");
            jdbc.execute("INSERT IGNORE INTO knowledge_document_audience_role (document_id, role_code) SELECT id, 'COACH' FROM knowledge_document WHERE tenant_id IS NULL OR tenant_id IS NOT NULL");
            jdbc.execute("INSERT IGNORE INTO knowledge_document_audience_role (document_id, role_code) SELECT id, 'MEMBER' FROM knowledge_document WHERE tenant_id IS NULL OR tenant_id IS NOT NULL");
        } catch (DataAccessException ignored) {
            // Hibernate or a non-MySQL development database may create the table later.
        }
    }
}
