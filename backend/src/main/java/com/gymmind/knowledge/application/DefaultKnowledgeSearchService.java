package com.gymmind.knowledge.application;

import com.gymmind.ai.domain.ContextBudgeter;
import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.domain.HybridRetriever;
import com.gymmind.ai.domain.RankedChunk;
import com.gymmind.ai.domain.RerankGateway;
import com.gymmind.ai.domain.EmbeddingGateway;
import com.gymmind.ai.harness.AiProgressEvent;
import com.gymmind.knowledge.infrastructure.search.KeywordIndexPort;
import com.gymmind.knowledge.infrastructure.search.VectorIndexPort;
import com.gymmind.knowledge.infrastructure.search.KnowledgeIndexingService;
import com.gymmind.knowledge.domain.model.DocumentStatus;
import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.tenancy.application.TenantQuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class DefaultKnowledgeSearchService implements KnowledgeSearchService {

    private static final int RECALL_TOP_K = 30;
    private static final int FUSION_TOP_K = 20;
    private static final int MAX_SEGMENTS_PER_DOCUMENT = 2;

    private final HybridRetriever retriever;
    private final RerankGateway rerank;
    private final TenantQuotaService quotas;
    private final KnowledgeDocumentRepository documents;

    public DefaultKnowledgeSearchService(
            KeywordIndexPort keyword,
            VectorIndexPort vector,
            EmbeddingGateway embedding,
            RerankGateway rerank,
            TenantQuotaService quotas) {
        this(keyword, vector, embedding, rerank, quotas, null);
    }

    @Autowired
    public DefaultKnowledgeSearchService(
            KeywordIndexPort keyword,
            VectorIndexPort vector,
            EmbeddingGateway embedding,
            RerankGateway rerank,
            TenantQuotaService quotas,
            KnowledgeDocumentRepository documents) {
        this.retriever = new HybridRetriever(keyword, vector, embedding);
        this.rerank = rerank;
        this.quotas = quotas;
        this.documents = documents;
    }

    @Override
    public List<ContextSegment> search(CurrentActor actor, String query) {
        return search(actor, query, ignored -> { });
    }

    @Override
    public List<ContextSegment> search(CurrentActor actor, String query,
                                       Consumer<AiProgressEvent> progress) {
        Consumer<AiProgressEvent> reporter = progress == null ? ignored -> { } : progress;
        if (actor == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Long tenantId = ActorAccess.tenantId(actor);
        if (!actor.isPlatformAdmin() && tenantId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (query == null || query.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (tenantId != null && quotas != null) quotas.ensureKnowledgeModuleEnabled(tenantId);

        emit(reporter, "permission_filter", AiProgressEvent.Status.RUNNING, "权限范围筛选中", null);
        List<Long> scopes = searchScopes(actor, tenantId);
        emit(reporter, "permission_filter", AiProgressEvent.Status.COMPLETED, "权限范围筛选完成", scopes.size());
        HybridRetriever.Result result = retriever.retrieve(query, scopes, actor.userId(), RECALL_TOP_K, FUSION_TOP_K, reporter);
        if (!result.available()) {
            throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE);
        }

        List<ContextSegment> fused = filterSearchable(actor, result);
        emit(reporter, "rerank", AiProgressEvent.Status.RUNNING, "重排序中", fused.size());
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
                    // A tenant document always outranks the platform default,
                    // even when the external reranker scores the default higher.
                    fused = prioritizeTenant(fused, result.chunksById(), tenantId);
                }
            } catch (RuntimeException ignored) {
                // Keep fused ranking when rerank is unavailable.
            }
        }

        fused = limitPerDocument(fused);
        emit(reporter, "rerank", AiProgressEvent.Status.COMPLETED, "重排序完成", fused.size());
        emit(reporter, "citation_prepare", AiProgressEvent.Status.RUNNING, "引用整理中", null);
        List<ContextSegment> selected = ContextBudgeter.select(fused, 12000);
        emit(reporter, "citation_prepare", AiProgressEvent.Status.COMPLETED, "引用整理完成", selected.size());
        return selected;
    }

    private static void emit(Consumer<AiProgressEvent> progress, String stage,
                             AiProgressEvent.Status status, String message, Integer resultCount) {
        progress.accept(new AiProgressEvent("knowledge_search", stage, status, message, resultCount));
    }

    private List<ContextSegment> filterSearchable(CurrentActor actor, HybridRetriever.Result result) {
        if (documents == null) return result.segments();
        Long tenantId = ActorAccess.tenantId(actor);
        var ids = result.chunksById().values().stream().map(com.gymmind.knowledge.infrastructure.search.IndexedChunk::documentId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, KnowledgeDocument> searchable = new HashMap<>();
        if (actor.isPlatformAdmin()) {
            documents.findAllDocuments().stream()
                    .filter(document -> ids.contains(document.id()))
                    .forEach(document -> searchable.put(document.id(), document));
        } else {
            if (tenantId != null) documents.findByTenantIdAndIdIn(tenantId, ids).forEach(document -> searchable.put(document.id(), document));
            documents.findPlatformDocuments().forEach(document -> searchable.put(document.id(), document));
        }
        searchable.values().removeIf(document -> document.status() != DocumentStatus.READY || !document.published()
                || (document.visibility() == DocumentVisibility.PRIVATE_USER
                    && !Objects.equals(document.ownerUserId(), actor.userId()))
                || (document.visibility() != DocumentVisibility.PRIVATE_USER
                    && !ActorAccess.canManageTenant(actor)
                    && document.tenantId() != null
                    && document.visibility() != DocumentVisibility.TENANT)
                || (!actor.isPlatformAdmin()
                    && document.effectiveAudienceRoles().stream().noneMatch(actor.roles()::contains)));
        return result.segments().stream().filter(segment -> segment.documentId() == null
                || searchable.containsKey(Long.valueOf(segment.documentId()))).toList();
    }

    private List<Long> searchScopes(CurrentActor actor, Long tenantId) {
        LinkedHashSet<Long> scopes = new LinkedHashSet<>();
        scopes.add(KnowledgeIndexingService.PLATFORM_INDEX_TENANT_ID);
        if (actor.isPlatformAdmin()) {
            if (documents != null) documents.findTenantIds().stream().filter(Objects::nonNull).forEach(scopes::add);
        } else if (tenantId != null) {
            scopes.add(tenantId);
        }
        return List.copyOf(scopes);
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

    private static List<ContextSegment> prioritizeTenant(List<ContextSegment> segments,
                                                          Map<String, com.gymmind.knowledge.infrastructure.search.IndexedChunk> chunks,
                                                          Long tenantId) {
        if (tenantId == null) return segments;
        return segments.stream().sorted((left, right) -> {
            var l = chunks.get(left.id());
            var r = chunks.get(right.id());
            boolean lTenant = l != null && tenantId.equals(l.tenantId());
            boolean rTenant = r != null && tenantId.equals(r.tenantId());
            return Boolean.compare(rTenant, lTenant);
        }).toList();
    }
}
