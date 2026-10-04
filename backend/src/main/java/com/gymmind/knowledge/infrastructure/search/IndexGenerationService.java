package com.gymmind.knowledge.infrastructure.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IndexGenerationService {
    private static final Logger log = LoggerFactory.getLogger(IndexGenerationService.class);
    private final KeywordIndexPort keyword;
    private final VectorIndexPort vector;
    private final Map<Long, String> published = new ConcurrentHashMap<>();
    public IndexGenerationService(KeywordIndexPort keyword, VectorIndexPort vector) { this.keyword = keyword; this.vector = vector; }

    public boolean publish(Long tenantId, String generation, List<IndexedChunk> chunks) {
        // Tenant 0 is the reserved platform-wide index used by public documents.
        if (tenantId == null || tenantId < 0 || generation == null || generation.isBlank() || chunks == null) return false;
        for (IndexedChunk c : chunks) if (c == null || !tenantId.equals(c.tenantId()) || c.id() == null || c.vector() == null || c.vector().length != 1024) return false;
        try {
            for (IndexedChunk c : chunks) keyword.upsert(c);
            for (IndexedChunk c : chunks) vector.upsert(c);
            published.put(tenantId, generation.trim());
            return true;
        } catch (RuntimeException ex) {
            log.error("Knowledge index publish failed: tenantId={}, generation={}, chunks={}",
                    tenantId, generation, chunks.size(), ex);
            return false;
        }
    }

    public String currentGeneration(Long tenantId) { return published.get(tenantId); }

    public void delete(Long tenantId, String chunkId) {
        if (tenantId == null || tenantId < 0 || chunkId == null || chunkId.isBlank()) throw new IllegalArgumentException("invalid delete target");
        try {
            keyword.delete(chunkId, tenantId);
        } catch (RuntimeException ex) {
            log.warn("Knowledge keyword index delete failed: tenantId={}, chunkId={}", tenantId, chunkId, ex);
        }
        try {
            vector.delete(chunkId, tenantId);
        } catch (RuntimeException ex) {
            log.warn("Knowledge vector index delete failed: tenantId={}, chunkId={}", tenantId, chunkId, ex);
        }
    }
}
