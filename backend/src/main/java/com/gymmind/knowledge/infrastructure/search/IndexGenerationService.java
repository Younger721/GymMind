package com.gymmind.knowledge.infrastructure.search;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IndexGenerationService {
    private final KeywordIndexPort keyword;
    private final VectorIndexPort vector;
    private final Map<Long, String> published = new ConcurrentHashMap<>();
    public IndexGenerationService(KeywordIndexPort keyword, VectorIndexPort vector) { this.keyword = keyword; this.vector = vector; }

    public boolean publish(Long tenantId, String generation, List<IndexedChunk> chunks) {
        if (tenantId == null || tenantId <= 0 || generation == null || generation.isBlank() || chunks == null) return false;
        for (IndexedChunk c : chunks) if (c == null || !tenantId.equals(c.tenantId()) || c.id() == null || c.vector() == null || c.vector().length != 1024) return false;
        try {
            for (IndexedChunk c : chunks) keyword.upsert(c);
            for (IndexedChunk c : chunks) vector.upsert(c);
            published.put(tenantId, generation.trim());
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public String currentGeneration(Long tenantId) { return published.get(tenantId); }

    public void delete(Long tenantId, String chunkId) {
        if (tenantId == null || tenantId <= 0 || chunkId == null || chunkId.isBlank()) throw new IllegalArgumentException("invalid delete target");
        keyword.delete(chunkId, tenantId);
        vector.delete(chunkId, tenantId);
    }
}
