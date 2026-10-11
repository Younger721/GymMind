package com.gymmind.ai.domain;

import com.gymmind.ai.harness.AiProgressEvent;
import com.gymmind.knowledge.infrastructure.search.IndexedChunk;
import com.gymmind.knowledge.infrastructure.search.KeywordIndexPort;
import com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter;
import com.gymmind.knowledge.infrastructure.search.VectorIndexPort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Parallel keyword + vector recall with RRF fusion. */
public final class HybridRetriever {

    private static final Logger LOG = Logger.getLogger(HybridRetriever.class.getName());

    private final KeywordIndexPort keyword;
    private final VectorIndexPort vector;
    private final EmbeddingGateway embedding;

    public HybridRetriever(KeywordIndexPort keyword, VectorIndexPort vector, EmbeddingGateway embedding) {
        this.keyword = keyword;
        this.vector = vector;
        this.embedding = embedding;
    }

    public Result retrieve(String query, Long tenantId, Long userId, int recallTopK, int fusionTopK) {
        return retrieve(query, tenantId == null ? List.of() : List.of(tenantId), userId, recallTopK, fusionTopK,
                ignored -> { });
    }

    public Result retrieve(String query, Long tenantId, Long userId, int recallTopK, int fusionTopK,
                           Consumer<AiProgressEvent> progress) {
        return retrieve(query, tenantId == null ? List.of() : List.of(tenantId), userId, recallTopK, fusionTopK,
                progress);
    }

    public Result retrieve(String query, List<Long> tenantIds, Long userId, int recallTopK, int fusionTopK) {
        return retrieve(query, tenantIds, userId, recallTopK, fusionTopK, ignored -> { });
    }

    public Result retrieve(String query, List<Long> tenantIds, Long userId, int recallTopK, int fusionTopK,
                           Consumer<AiProgressEvent> progress) {
        return retrieve(query, tenantIds, new KnowledgeRecallFilter(userId, java.util.Set.of(), false),
                recallTopK, fusionTopK, progress);
    }

    public Result retrieve(String query, List<Long> tenantIds, KnowledgeRecallFilter filter, int recallTopK,
                           int fusionTopK, Consumer<AiProgressEvent> progress) {
        List<IndexedChunk> keywordHits;
        List<IndexedChunk> vectorHits;
        boolean keywordAvailable = true;
        boolean vectorAvailable = true;
        Consumer<AiProgressEvent> reporter = progress == null ? ignored -> { } : progress;
        reporter.accept(new AiProgressEvent("knowledge_search", "keyword_recall",
                AiProgressEvent.Status.RUNNING, "关键词检索中", null));
        try {
            keywordHits = tenantIds.stream()
                    .flatMap(scope -> recallKeyword(query, scope, filter).stream()).toList();
        } catch (RuntimeException ex) {
            keywordHits = List.of();
            keywordAvailable = false;
            LOG.log(Level.WARNING, "Keyword retrieval failed and was skipped", ex);
            reporter.accept(new AiProgressEvent("knowledge_search", "keyword_recall",
                    AiProgressEvent.Status.FAILED, "关键词检索不可用，已跳过", 0));
        }
        if (keywordAvailable) {
            reporter.accept(new AiProgressEvent("knowledge_search", "keyword_recall",
                    AiProgressEvent.Status.COMPLETED, "关键词检索完成", keywordHits.size()));
        }
        reporter.accept(new AiProgressEvent("knowledge_search", "vector_recall",
                AiProgressEvent.Status.RUNNING, "向量检索中", null));
        try {
            float[] queryVector = embedding.embed(List.of(query)).get(0);
            vectorHits = tenantIds.stream()
                    .flatMap(scope -> recallVector(queryVector, scope, filter).stream()).toList();
        } catch (RuntimeException ex) {
            vectorHits = List.of();
            vectorAvailable = false;
            LOG.log(Level.WARNING, "Vector retrieval failed and was skipped", ex);
            reporter.accept(new AiProgressEvent("knowledge_search", "vector_recall",
                    AiProgressEvent.Status.FAILED, "向量检索不可用，已跳过", 0));
        }
        if (vectorAvailable) {
            reporter.accept(new AiProgressEvent("knowledge_search", "vector_recall",
                    AiProgressEvent.Status.COMPLETED, "向量检索完成", vectorHits.size()));
        }

        if (!keywordAvailable && !vectorAvailable) {
            return Result.unavailable();
        }

        List<RankedChunk> keywordRanked = toRanked(keywordHits, recallTopK);
        List<RankedChunk> vectorRanked = toRanked(vectorHits, recallTopK);
        Map<String, IndexedChunk> all = new HashMap<>();
        keywordHits.forEach(chunk -> all.put(chunk.id(), chunk));
        vectorHits.forEach(chunk -> all.put(chunk.id(), chunk));

        reporter.accept(new AiProgressEvent("knowledge_search", "fusion",
                AiProgressEvent.Status.RUNNING, "结果融合中", null));
        List<IndexedChunk> fusedChunks = RrfFusion.merge(keywordRanked, vectorRanked, fusionTopK).stream()
                .map(entry -> all.get(entry.id()))
                .filter(Objects::nonNull)
                .toList();
        List<ContextSegment> fused = fusedChunks.stream()
                .map(chunk -> new ContextSegment(chunk.id(),
                        chunk.documentId() == null ? chunk.id() : chunk.documentId().toString(), chunk.text()))
                .toList();
        reporter.accept(new AiProgressEvent("knowledge_search", "fusion",
                AiProgressEvent.Status.COMPLETED, "结果融合完成", fused.size()));
        return new Result(true, keywordAvailable, vectorAvailable, fused, all, keywordHits, vectorHits, fusedChunks);
    }

    private List<IndexedChunk> recallKeyword(String query, Long scope, KnowledgeRecallFilter filter) {
        if (shouldUseAudienceFilter(filter)) {
            return keyword.search(query, scope, filter);
        }
        return keyword.search(query, scope, filter == null ? null : filter.userId());
    }

    private List<IndexedChunk> recallVector(float[] queryVector, Long scope, KnowledgeRecallFilter filter) {
        if (shouldUseAudienceFilter(filter)) {
            return vector.search(queryVector, scope, filter);
        }
        return vector.search(queryVector, scope, filter == null ? null : filter.userId());
    }

    private static boolean shouldUseAudienceFilter(KnowledgeRecallFilter filter) {
        return filter != null && !filter.skipAudienceFilter()
                && filter.actorRoles() != null && !filter.actorRoles().isEmpty();
    }

    private static List<RankedChunk> toRanked(List<IndexedChunk> chunks, int limit) {
        List<RankedChunk> ranked = new ArrayList<>();
        int count = Math.min(limit, chunks.size());
        for (int i = 0; i < count; i++) {
            ranked.add(new RankedChunk(chunks.get(i).id(), i + 1));
        }
        return ranked;
    }

    public record Result(boolean available,
                         boolean keywordAvailable,
                         boolean vectorAvailable,
                         List<ContextSegment> segments,
                         Map<String, IndexedChunk> chunksById,
                         List<IndexedChunk> keywordHits,
                         List<IndexedChunk> vectorHits,
                         List<IndexedChunk> fusedHits) {
        public Result(boolean available, List<ContextSegment> segments, Map<String, IndexedChunk> chunksById) {
            this(available, available, available, segments, chunksById, List.of(), List.of(), List.of());
        }

        /** Compatibility alias for callers that name the merged recall list fusion hits. */
        public List<IndexedChunk> fusionHits() {
            return fusedHits;
        }

        /** 响应头 X-GymMind-Search-Degraded：keyword / vector / none */
        public String degradationHeaderValue() {
            if (!keywordAvailable && vectorAvailable) {
                return "keyword";
            }
            if (keywordAvailable && !vectorAvailable) {
                return "vector";
            }
            return "none";
        }

        public boolean degraded() {
            return available && !"none".equals(degradationHeaderValue());
        }

        public static Result unavailable() {
            return new Result(false, false, false, List.of(), Map.of(), List.of(), List.of(), List.of());
        }
    }
}
