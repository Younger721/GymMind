package com.gymmind.knowledge.infrastructure.search;

public record IndexedChunk(
        String id,
        Long tenantId,
        Long documentId,
        Long ownerUserId,
        String text,
        float[] vector,
        String knowledgeKey,
        int knowledgeVersion,
        String indexGeneration) {

    public IndexedChunk(String id, Long tenantId, Long documentId, Long ownerUserId, String text, float[] vector) {
        this(id, tenantId, documentId, ownerUserId, text, vector, null, 1, null);
    }

    public IndexedChunk(String id, Long tenantId, Long documentId, Long ownerUserId, String text, float[] vector,
                        String knowledgeKey, int knowledgeVersion) {
        this(id, tenantId, documentId, ownerUserId, text, vector, knowledgeKey, knowledgeVersion, null);
    }

    public IndexedChunk(String id, Long tenantId, Long ownerUserId, String text, float[] vector) {
        this(id, tenantId, null, ownerUserId, text, vector, null, 1, null);
    }

    public IndexedChunk(String id, Long tenantId, String text, float[] vector) {
        this(id, tenantId, null, null, text, vector, null, 1, null);
    }

    public IndexedChunk withIndexGeneration(String generation) {
        return new IndexedChunk(id, tenantId, documentId, ownerUserId, text, vector, knowledgeKey, knowledgeVersion,
                generation == null || generation.isBlank() ? indexGeneration : generation.trim());
    }

    public boolean visibleTo(Long userId) {
        return ownerUserId == null || ownerUserId.equals(userId);
    }

    /** 是否与文档当前 index_generation 指针一致（空指针/空 chunk 代视为兼容旧数据）。 */
    public boolean matchesIndexGeneration(String documentIndexGeneration) {
        if (documentIndexGeneration == null || documentIndexGeneration.isBlank()) {
            return true;
        }
        if (indexGeneration == null || indexGeneration.isBlank()) {
            return false;
        }
        return documentIndexGeneration.equals(indexGeneration);
    }
}
