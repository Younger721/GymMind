-- Run once before starting a production build with spring.jpa.hibernate.ddl-auto=validate.
-- Existing rows remain valid and are interpreted as GENERAL strategy version 1.
ALTER TABLE knowledge_document
    ADD COLUMN chunk_strategy VARCHAR(32) NULL,
    ADD COLUMN chunk_strategy_version INT NULL;

UPDATE knowledge_document
SET chunk_strategy = 'GENERAL'
WHERE chunk_strategy IS NULL OR chunk_strategy = '';

UPDATE knowledge_document
SET chunk_strategy_version = 1
WHERE chunk_strategy_version IS NULL OR chunk_strategy_version <= 0;
