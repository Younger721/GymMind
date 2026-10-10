package com.gymmind.knowledge.infrastructure.search;

import com.gymmind.ai.domain.EmbeddingGateway;
import com.gymmind.knowledge.application.KnowledgeChunkArtifact;
import com.gymmind.knowledge.infrastructure.parser.Chunk;
import com.gymmind.knowledge.infrastructure.parser.ChunkStrategy;
import com.gymmind.knowledge.infrastructure.parser.DeterministicChunker;
import com.gymmind.knowledge.infrastructure.parser.DocumentParserRegistry;
import com.gymmind.knowledge.infrastructure.parser.ParsedDocument;
import com.gymmind.knowledge.infrastructure.parser.StructuredChunker;
import com.gymmind.knowledge.infrastructure.parser.HeuristicTokenCounter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Parses a stored document and publishes one tenant-scoped generation to ES and Milvus. */
@Service
public class KnowledgeIndexingService {
    public static final long PLATFORM_INDEX_TENANT_ID = 0L;
    private final IndexGenerationService index;
    private final EmbeddingGateway embedding;
    private final DocumentParserRegistry parser;
    private final DeterministicChunker chunker;
    private final StructuredChunker structuredChunker;

    @Autowired
    public KnowledgeIndexingService(IndexGenerationService index, EmbeddingGateway embedding) {
        this(index, embedding, DocumentParserRegistry.standard(), new DeterministicChunker(1200, 180));
    }

    KnowledgeIndexingService(IndexGenerationService index, EmbeddingGateway embedding,
                             DocumentParserRegistry parser, DeterministicChunker chunker) {
        this.index = index;
        this.embedding = embedding;
        this.parser = parser;
        this.chunker = chunker;
        this.structuredChunker = new StructuredChunker(new HeuristicTokenCounter());
    }

    public boolean publish(Long tenantId, Long documentId, Long ownerUserId,
                           String fileName, String contentType, byte[] bytes) {
        return publishIndex(tenantId, documentId, ownerUserId, fileName, contentType, bytes).success();
    }

    public boolean publish(Long tenantId, Long documentId, Long ownerUserId,
                           String fileName, String contentType, byte[] bytes,
                           String strategyName, Integer strategyVersion) {
        return publishIndex(tenantId, documentId, ownerUserId, fileName, contentType, bytes, strategyName, strategyVersion)
                .success();
    }

    public boolean publish(Long tenantId, Long documentId, Long ownerUserId,
                           String fileName, String contentType, byte[] bytes,
                           String strategyName, Integer strategyVersion, String knowledgeKey, int knowledgeVersion) {
        return publishIndex(tenantId, documentId, ownerUserId, fileName, contentType, bytes, strategyName,
                strategyVersion, knowledgeKey, knowledgeVersion).success();
    }

    public IndexPublishResult publishIndex(Long tenantId, Long documentId, Long ownerUserId,
                                           String fileName, String contentType, byte[] bytes) {
        return publishIndex(tenantId, documentId, ownerUserId, fileName, contentType, bytes,
                ChunkStrategy.GENERAL.name(), 1);
    }

    public IndexPublishResult publishIndex(Long tenantId, Long documentId, Long ownerUserId,
                                           String fileName, String contentType, byte[] bytes,
                                           String strategyName, Integer strategyVersion) {
        return publishIndex(tenantId, documentId, ownerUserId, fileName, contentType, bytes, strategyName,
                strategyVersion, null, 1);
    }

    public IndexPublishResult publishIndex(Long tenantId, Long documentId, Long ownerUserId,
                                           String fileName, String contentType, byte[] bytes,
                                           String strategyName, Integer strategyVersion, String knowledgeKey,
                                           int knowledgeVersion) {
        if (documentId == null || bytes == null) {
            return IndexPublishResult.failed();
        }
        ChunkStrategy strategy = parseStrategy(strategyName);
        int version = strategyVersion == null || strategyVersion <= 0 ? 1 : strategyVersion;
        List<Chunk> parsedChunks = chunkFromBytes(documentId, fileName, contentType, bytes, strategy, version);
        return publishChunks(tenantId, documentId, ownerUserId, parsedChunks, strategy, version, knowledgeKey,
                knowledgeVersion);
    }

    public List<Chunk> chunkFromBytes(Long documentId, String fileName, String contentType, byte[] bytes,
                                      String strategyName, Integer strategyVersion) {
        if (documentId == null || bytes == null) {
            throw new IllegalArgumentException("document and bytes required");
        }
        ChunkStrategy strategy = parseStrategy(strategyName);
        int version = strategyVersion == null || strategyVersion <= 0 ? 1 : strategyVersion;
        return chunkFromBytes(documentId, fileName, contentType, bytes, strategy, version);
    }

    public IndexPublishResult publishFromArtifact(Long tenantId, Long documentId, Long ownerUserId,
                                                  KnowledgeChunkArtifact artifact) {
        if (artifact == null || documentId == null || !documentId.equals(artifact.documentId())) {
            return IndexPublishResult.failed();
        }
        ChunkStrategy strategy = parseStrategy(artifact.chunkStrategy());
        List<Chunk> chunks = artifact.chunks().stream()
                .map(entry -> new Chunk(entry.id(), String.valueOf(documentId), entry.ordinal(), entry.text()))
                .toList();
        return publishChunks(tenantId, documentId, ownerUserId, chunks, strategy,
                artifact.chunkStrategyVersion(), artifact.knowledgeKey(), artifact.knowledgeVersion());
    }

    private List<Chunk> chunkFromBytes(Long documentId, String fileName, String contentType, byte[] bytes,
                                       ChunkStrategy strategy, int version) {
        ParsedDocument structured = parser.parseStructured(fileName, contentType, bytes);
        try {
            return structuredChunker.chunk(structured, documentId.toString(), strategy, version);
        } catch (UnsupportedOperationException ex) {
            return chunker.chunk(structured.plainText(), documentId.toString());
        }
    }

    private IndexPublishResult publishChunks(Long tenantId, Long documentId, Long ownerUserId, List<Chunk> parsedChunks,
                                             ChunkStrategy strategy, int version, String knowledgeKey,
                                             int knowledgeVersion) {
        long indexTenantId = tenantId == null ? PLATFORM_INDEX_TENANT_ID : tenantId;
        List<float[]> vectors = embedding.embed(parsedChunks.stream().map(Chunk::text).toList());
        if (vectors.size() != parsedChunks.size()) {
            return IndexPublishResult.failed();
        }
        String generationKey = "document-" + documentId + "-" + strategy.name() + "-v" + version + "-"
                + System.currentTimeMillis();
        var indexed = new java.util.ArrayList<IndexedChunk>();
        for (int i = 0; i < parsedChunks.size(); i++) {
            indexed.add(new IndexedChunk(parsedChunks.get(i).id(), indexTenantId, documentId, ownerUserId,
                    parsedChunks.get(i).text(), vectors.get(i), knowledgeKey, knowledgeVersion, generationKey));
        }
        if (!index.publishWithRollback(indexTenantId, indexed)) {
            return IndexPublishResult.failed();
        }
        index.gcStaleIndexGenerations(indexTenantId, documentId, generationKey);
        return IndexPublishResult.ok(generationKey);
    }

    private static ChunkStrategy parseStrategy(String value) {
        if (value == null || value.isBlank()) {
            return ChunkStrategy.GENERAL;
        }
        try {
            return ChunkStrategy.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ChunkStrategy.GENERAL;
        }
    }

    public void deleteDocument(Long tenantId, Long documentId) {
        if (documentId == null) {
            return;
        }
        long indexTenantId = tenantId == null ? PLATFORM_INDEX_TENANT_ID : tenantId;
        index.deleteDocument(indexTenantId, documentId);
    }
}
