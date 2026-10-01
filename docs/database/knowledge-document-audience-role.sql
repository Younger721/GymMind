-- Role targets for knowledge documents. Hibernate creates this table in dev;
-- run this migration for existing production databases before enabling uploads.
CREATE TABLE IF NOT EXISTS knowledge_document_audience_role (
    document_id BIGINT NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    PRIMARY KEY (document_id, role_code),
    CONSTRAINT fk_knowledge_document_audience_role_document
        FOREIGN KEY (document_id) REFERENCES knowledge_document (id)
);

INSERT IGNORE INTO knowledge_document_audience_role (document_id, role_code)
SELECT id, 'PLATFORM_ADMIN' FROM knowledge_document WHERE tenant_id IS NULL;
INSERT IGNORE INTO knowledge_document_audience_role (document_id, role_code)
SELECT id, 'GYM_ADMIN' FROM knowledge_document;
INSERT IGNORE INTO knowledge_document_audience_role (document_id, role_code)
SELECT id, 'COACH' FROM knowledge_document;
INSERT IGNORE INTO knowledge_document_audience_role (document_id, role_code)
SELECT id, 'MEMBER' FROM knowledge_document;
