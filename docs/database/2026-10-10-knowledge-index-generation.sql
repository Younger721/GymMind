-- 知识库索引 generation 指针（与 ES/Milvus chunk.indexGeneration 对齐）
ALTER TABLE knowledge_document ADD COLUMN IF NOT EXISTS index_generation VARCHAR(128) NULL;
