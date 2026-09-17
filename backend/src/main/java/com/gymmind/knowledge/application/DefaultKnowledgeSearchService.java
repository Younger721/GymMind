package com.gymmind.knowledge.application;

import com.gymmind.ai.domain.ContextBudgeter;
import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.domain.HybridRetriever;
import com.gymmind.ai.domain.RankedChunk;
import com.gymmind.ai.domain.RerankGateway;
import com.gymmind.ai.domain.EmbeddingGateway;
import com.gymmind.knowledge.infrastructure.search.KeywordIndexPort;
import com.gymmind.knowledge.infrastructure.search.VectorIndexPort;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class DefaultKnowledgeSearchService implements KnowledgeSearchService {

    private static final int RECALL_TOP_K = 30;
    private static final int FUSION_TOP_K = 20;
    private static final int MAX_SEGMENTS_PER_DOCUMENT = 2;

    private final HybridRetriever retriever;
    private final RerankGateway rerank;

    public DefaultKnowledgeSearchService(KeywordIndexPort keyword, VectorIndexPort vector, EmbeddingGateway embedding) {
        this(keyword, vector, embedding, null);
    }

    @Autowired
    public DefaultKnowledgeSearchService(
            KeywordIndexPort keyword,
            VectorIndexPort vector,
            EmbeddingGateway embedding,
            RerankGateway rerank) {
        this.retriever = new HybridRetriever(keyword, vector, embedding);
        this.rerank = rerank;
    }

    @Override
    public List<ContextSegment> search(CurrentActor actor, String query) {
        if (actor == null || actor.tenantId() == null || actor.isPlatformAdmin()
                || !actor.hasPermission("knowledge:read")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (query == null || query.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        HybridRetriever.Result result = retriever.retrieve(query, actor.tenantId(), actor.userId(), RECALL_TOP_K, FUSION_TOP_K);
        if (!result.available()) {
            throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE);
        }

        List<ContextSegment> fused = result.segments();
        if (rerank != null && !fused.isEmpty()) {
            try {
                Map<String, ContextSegment> byId = new HashMap<>();
                fused.forEach(segment -> byId.put(segment.id(), segment));
                List<RankedChunk> reranked = rerank.rerank(query, fused);
                if (reranked != null && !reranked.isEmpty()) {
                    fused = reranked.stream()
                            .map(entry -> byId.get(entry.id()))
                            .filter(Objects::nonNull)
                            .toList();
                }
            } catch (RuntimeException ignored) {
                // Keep fused ranking when rerank is unavailable.
            }
        }

        fused = limitPerDocument(fused);
        return ContextBudgeter.select(fused, 12000);
    }

    private static List<ContextSegment> limitPerDocument(List<ContextSegment> segments) {
        List<ContextSegment> limited = new ArrayList<>();
        Map<String, Integer> perDocument = new HashMap<>();
        for (ContextSegment segment : segments) {
            String docKey = segment.documentId() == null ? segment.id() : segment.documentId();
            int used = perDocument.getOrDefault(docKey, 0);
            if (used >= MAX_SEGMENTS_PER_DOCUMENT) {
                continue;
            }
            perDocument.put(docKey, used + 1);
            limited.add(segment);
        }
        return limited;
    }
}
