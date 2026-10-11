package com.gymmind.knowledge.infrastructure.search;

import com.gymmind.iam.domain.model.RoleCode;

import java.util.List;
import java.util.Set;

public record IndexedChunk(
        String id,
        Long tenantId,
        Long documentId,
        Long ownerUserId,
        String text,
        float[] vector,
        String knowledgeKey,
        int knowledgeVersion,
        String indexGeneration,
        String visibility,
        List<String> audienceRoles) {

    public IndexedChunk(String id, Long tenantId, Long documentId, Long ownerUserId, String text, float[] vector) {
        this(id, tenantId, documentId, ownerUserId, text, vector, null, 1, null, null, List.of());
    }

    public IndexedChunk(String id, Long tenantId, Long documentId, Long ownerUserId, String text, float[] vector,
                        String knowledgeKey, int knowledgeVersion) {
        this(id, tenantId, documentId, ownerUserId, text, vector, knowledgeKey, knowledgeVersion, null, null, List.of());
    }

    public IndexedChunk(String id, Long tenantId, Long ownerUserId, String text, float[] vector) {
        this(id, tenantId, null, ownerUserId, text, vector, null, 1, null, null, List.of());
    }

    public IndexedChunk(String id, Long tenantId, String text, float[] vector) {
        this(id, tenantId, null, null, text, vector, null, 1, null, null, List.of());
    }

    public IndexedChunk withIndexGeneration(String generation) {
        return new IndexedChunk(id, tenantId, documentId, ownerUserId, text, vector, knowledgeKey, knowledgeVersion,
                generation == null || generation.isBlank() ? indexGeneration : generation.trim(),
                visibility, audienceRoles);
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

    /** 索引层 audienceRoles 与当前用户角色是否有交集；空 audienceRoles 视为旧索引兼容。 */
    public boolean visibleToAudience(Set<RoleCode> actorRoles) {
        if (audienceRoles == null || audienceRoles.isEmpty()) {
            return true;
        }
        if (actorRoles == null || actorRoles.isEmpty()) {
            return false;
        }
        for (String indexed : audienceRoles) {
            if (indexed == null || indexed.isBlank()) {
                continue;
            }
            for (RoleCode role : actorRoles) {
                if (role != null && indexed.equals(role.name())) {
                    return true;
                }
            }
        }
        return false;
    }
}
