-- 两阶段知识库入库：切片 artifact 引用
ALTER TABLE knowledge_document
    ADD COLUMN IF NOT EXISTS chunk_artifact_key VARCHAR(512) NULL;
