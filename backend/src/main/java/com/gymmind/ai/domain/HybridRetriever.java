package com.gymmind.ai.domain;

import com.gymmind.knowledge.infrastructure.search.IndexedChunk;
import com.gymmind.knowledge.infrastructure.search.KeywordIndexPort;
import com.gymmind.knowledge.infrastructure.search.VectorIndexPort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Parallel keyword + vector recall with RRF fusion. */
public final class HybridRetriever {

    private final KeywordIndexPort keyword;
    private final VectorIndexPort vector;
    private final EmbeddingGateway embedding;

    public HybridRetriever(KeywordIndexPort keyword, VectorIndexPort vector, EmbeddingGateway embedding) {
        this.keyword = keyword;
        this.vector = vector;
        this.embedding = embedding;
    }

    public Result retrieve(String query, Long tenantId, Long userId, int recallTopK, int fusionTopK) {
        List<IndexedChunk> keywordHits;
        List<IndexedChunk> vectorHits;
        try {
            keywordHits = keyword.search(query, tenantId, userId);
        } catch (RuntimeException ex) {
            keywordHits = List.of();
        }
        try {
            vectorHits = vector.search(embedding.embed(List.of(query)).get(0), tenantId, userId);
        } catch (RuntimeException ex) {
            vectorHits = List.of();
        }

        if (keywordHits.isEmpty() && vectorHits.isEmpty()) {
            return Result.unavailable();
        }

        List<RankedChunk> keywordRanked = toRanked(keywordHits, recallTopK);
        List<RankedChunk> vectorRanked = toRanked(vectorHits, recallTopK);
        Map<String, IndexedChunk> all = new HashMap<>();
        keywordHits.forEach(chunk -> all.put(chunk.id(), chunk));
        vectorHits.forEach(chunk -> all.put(chunk.id(), chunk));

        List<ContextSegment> fused = RrfFusion.merge(keywordRanked, vectorRanked, fusionTopK).stream()
                .map(entry -> all.get(entry.id()))
                .filter(Objects::nonNull)
                .map(chunk -> new ContextSegment(chunk.id(), chunk.id(), chunk.text()))
                .toList();
        return new Result(true, fused, all);
    }

    private static List<RankedChunk> toRanked(List<IndexedChunk> chunks, int limit) {
        List<RankedChunk> ranked = new ArrayList<>();
        int count = Math.min(limit, chunks.size());
        for (int i = 0; i < count; i++) {
            ranked.add(new RankedChunk(chunks.get(i).id(), i + 1));
        }
        return ranked;
    }

    public record Result(boolean available, List<ContextSegment> segments, Map<String, IndexedChunk> chunksById) {
        public static Result unavailable() {
            return new Result(false, List.of(), Map.of());
        }
    }
}
