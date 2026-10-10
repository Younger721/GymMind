package com.gymmind.knowledge.infrastructure.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IndexGenerationService {
    private static final Logger log = LoggerFactory.getLogger(IndexGenerationService.class);
    private final KeywordIndexPort keyword;
    private final VectorIndexPort vector;
    private final Map<Long, String> published = new ConcurrentHashMap<>();

    public IndexGenerationService(KeywordIndexPort keyword, VectorIndexPort vector) {
        this.keyword = keyword;
        this.vector = vector;
    }

    /**
     * 写入带 indexGeneration 的 chunk；Milvus 失败时回滚本批 ES upsert。
     */
    public boolean publishWithRollback(Long tenantId, List<IndexedChunk> chunks) {
        if (tenantId == null || tenantId < 0 || chunks == null || chunks.isEmpty()) {
            return false;
        }
        for (IndexedChunk chunk : chunks) {
            if (chunk == null || !tenantId.equals(chunk.tenantId()) || chunk.id() == null
                    || chunk.vector() == null || chunk.vector().length != 1024
                    || chunk.indexGeneration() == null || chunk.indexGeneration().isBlank()) {
                return false;
            }
        }
        try {
            for (IndexedChunk chunk : chunks) {
                keyword.upsert(chunk);
            }
        } catch (RuntimeException ex) {
            log.error("Knowledge keyword upsert failed: tenantId={}, chunks={}", tenantId, chunks.size(), ex);
            return false;
        }
        try {
            for (IndexedChunk chunk : chunks) {
                vector.upsert(chunk);
            }
        } catch (RuntimeException ex) {
            log.error("Knowledge vector upsert failed, rolling back keyword batch: tenantId={}, chunks={}",
                    tenantId, chunks.size(), ex);
            rollbackKeywordBatch(tenantId, chunks);
            return false;
        }
        String generation = chunks.get(0).indexGeneration();
        published.put(tenantId, generation);
        return true;
    }

    /** 兼容测试与旧调用：为 chunk 补上 generation 后走 publishWithRollback。 */
    public boolean publish(Long tenantId, String generation, List<IndexedChunk> chunks) {
        if (generation == null || generation.isBlank()) {
            return false;
        }
        List<IndexedChunk> stamped = chunks.stream().map(c -> c.withIndexGeneration(generation)).toList();
        return publishWithRollback(tenantId, stamped);
    }

    public void gcStaleIndexGenerations(Long tenantId, Long documentId, String keepGeneration) {
        if (tenantId == null || tenantId < 0 || documentId == null || documentId <= 0
                || keepGeneration == null || keepGeneration.isBlank()) {
            throw new IllegalArgumentException("invalid gc target");
        }
        try {
            keyword.deleteStaleIndexGenerations(tenantId, documentId, keepGeneration);
        } catch (RuntimeException ex) {
            log.warn("Keyword stale generation gc failed: tenantId={}, documentId={}", tenantId, documentId, ex);
        }
        try {
            vector.deleteStaleIndexGenerations(tenantId, documentId, keepGeneration);
        } catch (RuntimeException ex) {
            log.warn("Vector stale generation gc failed: tenantId={}, documentId={}", tenantId, documentId, ex);
        }
    }

    public String currentGeneration(Long tenantId) {
        return published.get(tenantId);
    }

    public void delete(Long tenantId, String chunkId) {
        if (tenantId == null || tenantId < 0 || chunkId == null || chunkId.isBlank()) {
            throw new IllegalArgumentException("invalid delete target");
        }
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

    public void deleteDocument(Long tenantId, Long documentId) {
        if (tenantId == null || tenantId < 0 || documentId == null || documentId <= 0) {
            throw new IllegalArgumentException("invalid delete target");
        }
        keyword.deleteDocument(tenantId, documentId);
        vector.deleteDocument(tenantId, documentId);
    }

    private void rollbackKeywordBatch(Long tenantId, List<IndexedChunk> chunks) {
        for (IndexedChunk chunk : chunks) {
            try {
                keyword.delete(chunk.id(), tenantId);
            } catch (RuntimeException ex) {
                log.warn("Keyword rollback delete failed: tenantId={}, chunkId={}", tenantId, chunk.id(), ex);
            }
        }
    }
}
