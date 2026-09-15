package com.gymmind.knowledge.infrastructure.search;

import com.gymmind.ai.domain.EmbeddingGateway;
import com.gymmind.knowledge.infrastructure.parser.Chunk;
import com.gymmind.knowledge.infrastructure.parser.DeterministicChunker;
import com.gymmind.knowledge.infrastructure.parser.DocumentParserRegistry;
import java.util.List;
import org.springframework.stereotype.Service;

/** Parses a stored document and publishes one tenant-scoped generation to ES and Milvus. */
@Service
public class KnowledgeIndexingService {
    private final IndexGenerationService index;
    private final EmbeddingGateway embedding;
    private final DocumentParserRegistry parser;
    private final DeterministicChunker chunker;

    public KnowledgeIndexingService(IndexGenerationService index, EmbeddingGateway embedding) {
        this(index, embedding, DocumentParserRegistry.standard(), new DeterministicChunker(1200, 180));
    }

    KnowledgeIndexingService(IndexGenerationService index, EmbeddingGateway embedding,
                             DocumentParserRegistry parser, DeterministicChunker chunker) {
        this.index = index; this.embedding = embedding; this.parser = parser; this.chunker = chunker;
    }

    public boolean publish(Long tenantId, Long documentId, Long ownerUserId,
                           String fileName, String contentType, byte[] bytes) {
        if (tenantId == null || documentId == null || bytes == null) return false;
        String text = parser.parse(fileName, contentType, bytes);
        List<Chunk> chunks = chunker.chunk(text, documentId.toString());
        List<float[]> vectors = embedding.embed(chunks.stream().map(Chunk::text).toList());
        if (vectors.size() != chunks.size()) return false;
        var indexed = new java.util.ArrayList<IndexedChunk>();
        for (int i = 0; i < chunks.size(); i++) {
            indexed.add(new IndexedChunk(chunks.get(i).id(), tenantId, ownerUserId, chunks.get(i).text(), vectors.get(i)));
        }
        return index.publish(tenantId, "document-" + documentId, indexed);
    }
}
