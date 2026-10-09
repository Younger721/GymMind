package com.gymmind.knowledge.infrastructure.search;

public record IndexedChunk(String id, Long tenantId, Long documentId, Long ownerUserId, String text, float[] vector, String knowledgeKey, int knowledgeVersion) {
    public IndexedChunk(String id, Long tenantId, Long documentId, Long ownerUserId, String text, float[] vector) { this(id, tenantId, documentId, ownerUserId, text, vector, null, 1); }
    public IndexedChunk(String id, Long tenantId, Long ownerUserId, String text, float[] vector) {
        this(id, tenantId, null, ownerUserId, text, vector, null, 1);
    }

    public IndexedChunk(String id, Long tenantId, String text, float[] vector) {
        this(id, tenantId, null, null, text, vector, null, 1);
    }

    public boolean visibleTo(Long userId) {
        return ownerUserId == null || ownerUserId.equals(userId);
    }
}
