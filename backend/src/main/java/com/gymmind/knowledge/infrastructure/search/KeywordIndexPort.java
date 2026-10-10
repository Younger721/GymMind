package com.gymmind.knowledge.infrastructure.search;

import java.util.List;

public interface KeywordIndexPort {
    void upsert(IndexedChunk chunk);

    List<IndexedChunk> search(String query, Long tenantId);

    default List<IndexedChunk> search(String query, List<Long> tenantIds, Long userId) {
        return tenantIds.stream().flatMap(t -> search(query, t).stream()).filter(c -> c.visibleTo(userId)).toList();
    }

    default List<IndexedChunk> search(String query, Long tenantId, Long userId) {
        return search(query, tenantId).stream().filter(c -> c.visibleTo(userId)).toList();
    }

    void delete(String id, Long tenantId);

    default void deleteDocument(Long tenantId, Long documentId) {
        throw new UnsupportedOperationException("document deletion is not supported");
    }

    /** 删除同文档下 indexGeneration 不等于 keepGeneration 的 chunk（含无 generation 的旧数据）。 */
    default void deleteStaleIndexGenerations(Long tenantId, Long documentId, String keepGeneration) {
        throw new UnsupportedOperationException("stale generation gc is not supported");
    }
}
