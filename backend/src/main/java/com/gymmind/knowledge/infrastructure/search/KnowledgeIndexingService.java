package com.gymmind.knowledge.infrastructure.search;

import com.gymmind.ai.domain.EmbeddingGateway;
import com.gymmind.knowledge.application.KnowledgeChunkRepository;
import com.gymmind.knowledge.domain.model.KnowledgeChunk;
import com.gymmind.knowledge.infrastructure.parser.Chunk;
import com.gymmind.knowledge.infrastructure.parser.DeterministicChunker;
import com.gymmind.knowledge.infrastructure.parser.DocumentParserRegistry;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Parses a stored document and publishes one tenant-scoped generation to ES and Milvus. */
@Service
public class KnowledgeIndexingService {
    private final IndexGenerationService index;
    private final EmbeddingGateway embedding;
    private final DocumentParserRegistry parser;
    private final DeterministicChunker chunker;
    private final KnowledgeChunkRepository chunkRepository;

    @Autowired
    public KnowledgeIndexingService(IndexGenerationService index, EmbeddingGateway embedding,
                                    KnowledgeChunkRepository chunkRepository) {
        this(index, embedding, DocumentParserRegistry.standard(), new DeterministicChunker(1200, 180), chunkRepository);
    }

    KnowledgeIndexingService(IndexGenerationService index, EmbeddingGateway embedding,
                             DocumentParserRegistry parser, DeterministicChunker chunker,
                             KnowledgeChunkRepository chunkRepository) {
        this.index = index;
        this.embedding = embedding;
        this.parser = parser;
        this.chunker = chunker;
        this.chunkRepository = chunkRepository;
    }

    public boolean publish(Long tenantId, Long documentId, Long ownerUserId,
                           String fileName, String contentType, byte[] bytes) {
        if (tenantId == null || documentId == null || bytes == null) return false;
        String text = parser.parse(fileName, contentType, bytes);
        List<Chunk> parsedChunks = chunker.chunk(text, documentId.toString());
        List<float[]> vectors = embedding.embed(parsedChunks.stream().map(Chunk::text).toList());
        if (vectors.size() != parsedChunks.size()) return false;
        var indexed = new java.util.ArrayList<IndexedChunk>();
        for (int i = 0; i < parsedChunks.size(); i++) {
            indexed.add(new IndexedChunk(parsedChunks.get(i).id(), tenantId, ownerUserId,
                    parsedChunks.get(i).text(), vectors.get(i)));
        }
        long generation = System.currentTimeMillis();
        String generationKey = "document-" + documentId + "-" + generation;
        if (!index.publish(tenantId, generationKey, indexed)) {
            return false;
        }
        if (chunkRepository != null) {
            chunkRepository.deleteByTenantIdAndDocumentId(tenantId, documentId);
            for (Chunk chunk : parsedChunks) {
                chunkRepository.save(KnowledgeChunk.create(tenantId, documentId, chunk.id(), generation, chunk.text()));
            }
        }
        return true;
    }
}
