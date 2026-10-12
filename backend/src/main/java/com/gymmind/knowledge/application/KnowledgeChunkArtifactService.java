package com.gymmind.knowledge.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import com.gymmind.knowledge.infrastructure.parser.Chunk;
import com.gymmind.knowledge.infrastructure.parser.ChunkStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class KnowledgeChunkArtifactService {

    private static final int MAX_ARTIFACT_BYTES = 32 * 1024 * 1024;

    private final KnowledgeObjectStore store;
    private final ObjectMapper mapper;
    private final Clock clock;

    @Autowired
    public KnowledgeChunkArtifactService(KnowledgeObjectStore store, ObjectMapper mapper, Clock clock) {
        this.store = store;
        this.mapper = mapper.copy().findAndRegisterModules();
        this.clock = clock;
    }

    KnowledgeChunkArtifactService(KnowledgeObjectStore store, ObjectMapper mapper) {
        this(store, mapper, Clock.systemUTC());
    }

    /** 根据原文件 objectKey 与 generation 生成 chunk artifact 路径 */
    public static String resolveArtifactKey(String sourceObjectKey, String generation) {
        if (sourceObjectKey == null || sourceObjectKey.isBlank() || generation == null || generation.isBlank()) {
            throw new IllegalArgumentException("invalid artifact key inputs");
        }
        int slash = sourceObjectKey.lastIndexOf('/');
        if (slash < 0) {
            throw new IllegalArgumentException("invalid source object key");
        }
        String safeGeneration = generation.replace('/', '_');
        return sourceObjectKey.substring(0, slash + 1) + "chunks/" + safeGeneration + ".json";
    }

    public static String generationLabel(KnowledgeDocument document) {
        ChunkStrategy strategy = parseStrategy(document.chunkStrategy());
        return strategy.name() + "-v" + document.chunkStrategyVersion() + "-" + System.currentTimeMillis();
    }

    public String writeArtifact(KnowledgeDocument document, List<Chunk> chunks) {
        String generation = generationLabel(document);
        String artifactKey = resolveArtifactKey(document.objectKey(), generation);
        KnowledgeChunkArtifact artifact = new KnowledgeChunkArtifact(
                KnowledgeChunkArtifact.CURRENT_VERSION,
                document.id(),
                document.tenantId(),
                document.ownerUserId(),
                document.knowledgeKey(),
                document.knowledgeVersion(),
                document.chunkStrategy(),
                document.chunkStrategyVersion(),
                document.objectKey(),
                document.contentType(),
                document.fileName(),
                clock.instant().toEpochMilli(),
                chunks.stream()
                        .map(chunk -> new KnowledgeChunkArtifact.ChunkEntry(chunk.id(), chunk.ordinal(), chunk.text()))
                        .toList());
        byte[] json;
        try {
            json = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(artifact);
        } catch (Exception ex) {
            throw new IllegalStateException("chunk artifact serialization failed", ex);
        }
        if (json.length > MAX_ARTIFACT_BYTES) {
            throw new IllegalStateException("chunk artifact exceeds size limit");
        }
        store.put(artifactKey, json, "application/json; charset=utf-8");
        return artifactKey;
    }

    public KnowledgeChunkArtifact readArtifact(String artifactKey) {
        if (artifactKey == null || artifactKey.isBlank()) {
            throw new IllegalArgumentException("artifact key required");
        }
        byte[] bytes = store.read(artifactKey);
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("chunk artifact is unavailable");
        }
        try {
            KnowledgeChunkArtifact artifact = mapper.readValue(bytes, KnowledgeChunkArtifact.class);
            validateArtifact(artifact);
            return artifact;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("chunk artifact parse failed", ex);
        }
    }

    public void deleteArtifact(String artifactKey) {
        if (artifactKey == null || artifactKey.isBlank()) {
            return;
        }
        store.delete(artifactKey);
    }

    private static void validateArtifact(KnowledgeChunkArtifact artifact) {
        if (artifact == null || artifact.chunks() == null || artifact.chunks().isEmpty()) {
            throw new IllegalStateException("empty chunk artifact");
        }
        if (artifact.artifactVersion() != KnowledgeChunkArtifact.CURRENT_VERSION) {
            throw new IllegalStateException("unsupported artifact version");
        }
    }

    private static ChunkStrategy parseStrategy(String value) {
        if (value == null || value.isBlank()) {
            return ChunkStrategy.GENERAL;
        }
        try {
            return ChunkStrategy.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return ChunkStrategy.GENERAL;
        }
    }
}
