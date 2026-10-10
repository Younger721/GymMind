package com.gymmind.knowledge.application;

import java.util.List;

/** 切片 checkpoint 的 JSON 结构（v1），持久化在 MinIO。 */
public record KnowledgeChunkArtifact(
        int artifactVersion,
        long documentId,
        Long tenantId,
        Long ownerUserId,
        String knowledgeKey,
        int knowledgeVersion,
        String chunkStrategy,
        int chunkStrategyVersion,
        String sourceObjectKey,
        String sourceContentType,
        String sourceFileName,
        long createdAtEpochMilli,
        List<ChunkEntry> chunks) {

    public record ChunkEntry(String id, int ordinal, String text) {
    }

    public static final int CURRENT_VERSION = 1;
}
