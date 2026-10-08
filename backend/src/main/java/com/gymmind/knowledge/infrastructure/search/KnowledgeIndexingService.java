package com.gymmind.knowledge.infrastructure.search;

import com.gymmind.ai.domain.EmbeddingGateway;
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
        return publish(tenantId, documentId, ownerUserId, fileName, contentType, bytes, ChunkStrategy.GENERAL.name(), 1);
    }

    /**
     * Parse and publish one stable generation using the document's saved chunking strategy.
     * The legacy overload above intentionally remains for callers and historical jobs that
     * have not yet loaded document metadata.
     */
    public boolean publish(Long tenantId, Long documentId, Long ownerUserId,
                           String fileName, String contentType, byte[] bytes,
                           String strategyName, Integer strategyVersion) {
        if (documentId == null || bytes == null) return false;
        long indexTenantId = tenantId == null ? PLATFORM_INDEX_TENANT_ID : tenantId;
        ChunkStrategy strategy = parseStrategy(strategyName);
        int version = strategyVersion == null || strategyVersion <= 0 ? 1 : strategyVersion;
        ParsedDocument structured = parser.parseStructured(fileName, contentType, bytes);
        List<Chunk> parsedChunks;
        try {
            parsedChunks = structuredChunker.chunk(structured, documentId.toString(), strategy, version);
        } catch (UnsupportedOperationException ex) {
            // Keep the deterministic character chunker as a compatibility fallback if a
            // custom parser/chunker implementation cannot provide structured blocks.
            parsedChunks = chunker.chunk(structured.plainText(), documentId.toString());
        }
        List<float[]> vectors = embedding.embed(parsedChunks.stream().map(Chunk::text).toList());
        if (vectors.size() != parsedChunks.size()) return false;
        var indexed = new java.util.ArrayList<IndexedChunk>();
        for (int i = 0; i < parsedChunks.size(); i++) {
            indexed.add(new IndexedChunk(parsedChunks.get(i).id(), indexTenantId, documentId, ownerUserId,
                    parsedChunks.get(i).text(), vectors.get(i)));
        }
        long generation = System.currentTimeMillis();
        String generationKey = "document-" + documentId + "-" + strategy.name() + "-v" + version + "-" + generation;
        index.deleteDocument(indexTenantId, documentId);
        return index.publish(indexTenantId, generationKey, indexed);
    }

    private static ChunkStrategy parseStrategy(String value) {
        if (value == null || value.isBlank()) return ChunkStrategy.GENERAL;
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
