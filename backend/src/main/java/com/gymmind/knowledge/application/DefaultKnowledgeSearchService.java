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
import com.gymmind.knowledge.infrastructure.search.IndexedChunk;
import com.gymmind.knowledge.infrastructure.search.KnowledgeRecallFilter;
import com.gymmind.knowledge.domain.model.DocumentStatus;
import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.tenancy.application.TenantQuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Service
public class DefaultKnowledgeSearchService implements KnowledgeSearchService {

    private static final int RECALL_TOP_K = 30;
    private static final int FUSION_TOP_K = 20;
    private static final int MAX_SEGMENTS_PER_DOCUMENT = 3;
    private static final int MAX_PROGRESS_DETAILS = 20;
    private static final int MAX_PROGRESS_EXCERPT = 300;
    private static final Pattern QUERY_PARTS = Pattern.compile("[^\\p{IsHan}A-Za-z0-9]+");

    private final HybridRetriever retriever;
    private final RerankGateway rerank;
    private final TenantQuotaService quotas;
    private final KnowledgeDocumentRepository documents;
    private final boolean requireDualRecall;
    private final KnowledgeSearchDegradationContext degradationContext;
    private final SearchDegradationMetrics degradationMetrics;

    public DefaultKnowledgeSearchService(
            KeywordIndexPort keyword,
            VectorIndexPort vector,
            EmbeddingGateway embedding,
            RerankGateway rerank,
            TenantQuotaService quotas) {
        this(keyword, vector, embedding, rerank, quotas, null);
    }

    public DefaultKnowledgeSearchService(
            KeywordIndexPort keyword,
            VectorIndexPort vector,
            EmbeddingGateway embedding,
            RerankGateway rerank,
            TenantQuotaService quotas,
            KnowledgeDocumentRepository documents) {
        this(keyword, vector, embedding, rerank, quotas, documents, false, null, null);
    }

    @Autowired
    public DefaultKnowledgeSearchService(
            KeywordIndexPort keyword,
            VectorIndexPort vector,
            EmbeddingGateway embedding,
            RerankGateway rerank,
            TenantQuotaService quotas,
            KnowledgeDocumentRepository documents,
            @org.springframework.beans.factory.annotation.Value("${gymmind.search.require-dual-recall:false}") boolean requireDualRecall,
            @org.springframework.beans.factory.annotation.Autowired(required = false) KnowledgeSearchDegradationContext degradationContext,
            @org.springframework.beans.factory.annotation.Autowired(required = false) SearchDegradationMetrics degradationMetrics) {
        this.retriever = new HybridRetriever(keyword, vector, embedding);
        this.rerank = rerank;
        this.quotas = quotas;
        this.documents = documents;
        this.requireDualRecall = requireDualRecall;
        this.degradationContext = degradationContext;
        this.degradationMetrics = degradationMetrics;
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
        if (degradationContext != null) {
            degradationContext.beginSearch();
        }

        emit(reporter, "permission_filter", AiProgressEvent.Status.RUNNING, "权限范围筛选中", null);
        List<Long> scopes = searchScopes(actor, tenantId);
        emit(reporter, "permission_filter", AiProgressEvent.Status.COMPLETED, "权限范围筛选完成", scopes.size(),
                permissionDetails(actor, scopes));
        // Retrieval itself only reports non-sensitive running/failure states.
        // Completed recall details are emitted below, after document visibility
        // and audience filters have been applied.
        Set<String> failedRecallStages = new HashSet<>();
        HybridRetriever.Result result = retriever.retrieve(query, scopes, KnowledgeRecallFilter.of(actor),
                RECALL_TOP_K, FUSION_TOP_K,
                event -> {
                    if (event.status() == AiProgressEvent.Status.FAILED) {
                        failedRecallStages.add(event.stage());
                    }
                    if (event.status() == AiProgressEvent.Status.RUNNING
                            || event.status() == AiProgressEvent.Status.FAILED) {
                        reporter.accept(event);
                    }
                });
        if (!result.available()) {
            throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE);
        }
        if (requireDualRecall && (!result.keywordAvailable() || !result.vectorAvailable())) {
            throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE);
        }
        recordDegradation(result, reporter, failedRecallStages);

        Map<Long, KnowledgeDocument> searchableDocuments = searchableDocuments(actor, result);
        Set<String> authorizedChunkIds = authorizedChunkIds(result, searchableDocuments);
        if (!failedRecallStages.contains("keyword_recall")) {
            emit(reporter, "keyword_recall", AiProgressEvent.Status.COMPLETED, "关键词检索完成",
                    authorizedCount(result.keywordHits(), authorizedChunkIds),
                    chunkDetails(result.keywordHits(), authorizedChunkIds, searchableDocuments, "关键词召回"));
        }
        if (!failedRecallStages.contains("vector_recall")) {
            emit(reporter, "vector_recall", AiProgressEvent.Status.COMPLETED, "向量检索完成",
                    authorizedCount(result.vectorHits(), authorizedChunkIds),
                    chunkDetails(result.vectorHits(), authorizedChunkIds, searchableDocuments, "向量召回"));
        }

        List<ContextSegment> fused = filterSearchable(actor, result, searchableDocuments);
        emit(reporter, "fusion", AiProgressEvent.Status.COMPLETED, "结果融合完成", fused.size(),
                segmentDetails(fused, result.chunksById(), searchableDocuments, "融合候选"));
        fused = filterLexicalEvidence(query, fused);
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
                } else {
                    fused = localRerank(query, fused);
                }
            } catch (RuntimeException ignored) {
                // Keep useful lexical evidence when the external reranker is
                // unavailable or its endpoint/model is unsupported.
                fused = localRerank(query, fused);
            }
        } else {
            fused = localRerank(query, fused);
        }

        // Structured price evidence is appended after lexical/reranker stages so
        // a short promotion notice cannot be discarded for having a weak vector.
        fused = addStructuredPriceContext(actor, query, fused, searchableDocuments);
        fused = limitPerDocument(fused);
        emit(reporter, "rerank", AiProgressEvent.Status.COMPLETED, "重排序完成", fused.size(),
                segmentDetails(fused, result.chunksById(), searchableDocuments, "重排序结果"));
        emit(reporter, "citation_prepare", AiProgressEvent.Status.RUNNING, "引用整理中", null);
        List<ContextSegment> selected = ContextBudgeter.select(fused, 12000);
        emit(reporter, "citation_prepare", AiProgressEvent.Status.COMPLETED, "引用整理完成", selected.size(),
                segmentDetails(selected, result.chunksById(), searchableDocuments, "引用片段"));
        return selected;
    }

    private List<ContextSegment> addStructuredPriceContext(CurrentActor actor, String query,
                                                            List<ContextSegment> existing,
                                                            Map<Long, KnowledgeDocument> searchable) {
        if (documents == null || !isPriceQuery(query)) return existing;
        Long tenantId = ActorAccess.tenantId(actor);
        List<KnowledgeDocument> candidates = new ArrayList<>();
        if (tenantId != null) candidates.addAll(documents.findPriceRules(tenantId));
        if (tenantId != null || actor.isPlatformAdmin()) candidates.addAll(documents.findPriceRules(null));
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        List<KnowledgeDocument> currentPublished = candidates.stream()
                .filter(d -> d.status() == DocumentStatus.READY && d.published())
                .collect(Collectors.collectingAndThen(Collectors.toMap(
                        d -> (d.tenantId() == null ? "platform" : d.tenantId()) + "|"
                                + (d.knowledgeKey() == null ? "document-" + d.id() : d.knowledgeKey()),
                        d -> d, (left, right) -> left.knowledgeVersion() >= right.knowledgeVersion() ? left : right),
                        map -> new ArrayList<>(map.values())));
        List<KnowledgeDocument> accessible = currentPublished.stream()
                .filter(d -> d.tenantId() == null || d.tenantId().equals(tenantId))
                .filter(d -> d.visibility() != DocumentVisibility.PRIVATE_USER
                        && (actor.isPlatformAdmin() || d.effectiveAudienceRoles().stream().anyMatch(actor.roles()::contains)))
                .toList();
        String normalizedQuery = normalizeProduct(query);
        List<String> matchedProducts = accessible.stream()
                .filter(d -> d.knowledgeType() == com.gymmind.knowledge.domain.model.KnowledgeType.BASE_PRICE)
                .map(KnowledgeDocument::applicableProduct).filter(Objects::nonNull).distinct()
                .filter(product -> normalizedQuery.contains(normalizeProduct(product))).toList();
        List<KnowledgeDocument> relevant = new ArrayList<>();
        String answer;
        if (matchedProducts.size() != 1) {
            List<String> choices = accessible.stream()
                    .filter(d -> d.knowledgeType() == com.gymmind.knowledge.domain.model.KnowledgeType.BASE_PRICE)
                    .map(KnowledgeDocument::applicableProduct).filter(Objects::nonNull).distinct().sorted().toList();
            answer = choices.isEmpty()
                    ? "当前没有已发布且可确认的结构化价目。"
                    : "请先确认卡种" + (matchedProducts.size() > 1 ? "（问题中包含多个卡种）" : "")
                    + "，当前可查询的卡种有：" + String.join("、", choices) + "。";
            relevant.addAll(accessible.stream().filter(d ->
                    d.knowledgeType() == com.gymmind.knowledge.domain.model.KnowledgeType.BASE_PRICE).toList());
        } else {
            String product = matchedProducts.get(0);
            List<KnowledgeDocument> prices = accessible.stream()
                    .filter(d -> d.knowledgeType() == com.gymmind.knowledge.domain.model.KnowledgeType.BASE_PRICE)
                    .filter(d -> product.equalsIgnoreCase(d.applicableProduct())).toList();
            List<KnowledgeDocument> activePromotions = accessible.stream()
                    .filter(d -> d.knowledgeType() == com.gymmind.knowledge.domain.model.KnowledgeType.PROMOTION_RULE)
                    .filter(d -> product.equalsIgnoreCase(d.applicableProduct()))
                    .filter(d -> d.effectiveFrom() != null && d.effectiveTo() != null
                            && !now.isBefore(d.effectiveFrom()) && now.isBefore(d.effectiveTo()))
                    .sorted(Comparator.comparingInt(KnowledgeDocument::promotionPriority).reversed()).toList();
            List<KnowledgeDocument> undatedPromotions = accessible.stream()
                    .filter(d -> d.knowledgeType() == com.gymmind.knowledge.domain.model.KnowledgeType.PROMOTION_RULE)
                    .filter(d -> product.equalsIgnoreCase(d.applicableProduct()))
                    .filter(d -> d.effectiveFrom() == null || d.effectiveTo() == null).toList();
            relevant.addAll(prices);
            relevant.addAll(activePromotions);
            relevant.addAll(undatedPromotions);
            if (prices.isEmpty()) {
                answer = "未找到已发布且可确认的" + product + "基础价格；暂不能计算应付价格。";
            } else {
                List<KnowledgeDocument> distinctPrices = prices.stream()
                        .collect(Collectors.collectingAndThen(Collectors.toMap(
                                d -> d.priceAmount().stripTrailingZeros().toPlainString(), d -> d,
                                (left, right) -> left), map -> new ArrayList<>(map.values())));
                if (distinctPrices.size() > 1) {
                    answer = "当前" + product + "存在多个已发布价格，需人工确认后才能报价："
                            + sourceNames(prices) + "。";
                } else if (!undatedPromotions.isEmpty()) {
                    answer = "当前" + product + "基础价格为" + prices.get(0).priceAmount().toPlainString()
                            + "元，但存在未注明完整有效期的促销规则，需人工确认优惠后再报价。";
                } else if (!activePromotions.isEmpty() && activePromotions.get(0).promotionConditions() != null) {
                    answer = "当前" + product + "基础价格为" + prices.get(0).priceAmount().toPlainString()
                            + "元；促销规则含有未结构化适用条件，需人工确认后才能确定优惠价。";
                } else if (!activePromotions.isEmpty()
                        && activePromotions.size() > 1
                        && activePromotions.get(0).promotionPriority() == activePromotions.get(1).promotionPriority()) {
                    List<KnowledgeDocument> conflicts = activePromotions.stream()
                            .filter(d -> d.promotionPriority() == activePromotions.get(0).promotionPriority()).toList();
                    relevant.clear();
                    relevant.addAll(prices);
                    relevant.addAll(conflicts);
                    answer = "当前" + product + "存在同优先级促销冲突，需人工确认：" + sourceNames(conflicts) + "。";
                } else if (activePromotions.isEmpty()) {
                    answer = "根据《" + prices.get(0).fileName() + "》v" + prices.get(0).knowledgeVersion()
                            + "，" + product + "当前价格为" + prices.get(0).priceAmount().toPlainString()
                            + "元；没有检索到当前有效的促销规则。";
                } else {
                    KnowledgeDocument base = prices.get(0);
                    KnowledgeDocument promotion = activePromotions.get(0);
                    BigDecimal finalPrice = calculatePrice(base.priceAmount(), promotion);
                    answer = "根据《" + base.fileName() + "》v" + base.knowledgeVersion() + "，" + product
                            + "原价为" + base.priceAmount().toPlainString() + "元；根据《" + promotion.fileName()
                            + "》v" + promotion.knowledgeVersion() + "，活动优惠为" + promotionDescription(promotion)
                            + "，有效期" + promotion.effectiveFrom() + "至" + promotion.effectiveTo()
                            + "。按原价计算：" + base.priceAmount().toPlainString() + "元 → "
                            + finalPrice.toPlainString() + "元（不与其他优惠叠加）。";
                }
            }
        }
        List<ContextSegment> result = new ArrayList<>();
        result.add(new ContextSegment("price-answer:" + (tenantId == null ? "platform" : tenantId) + ":" + System.nanoTime(),
                "PRICE_RESULT", answer, null, null, "PRICE_RESULT", null, null, null, true));
        relevant.stream().collect(Collectors.toMap(KnowledgeDocument::id, d -> d, (left, right) -> left,
                        java.util.LinkedHashMap::new)).values()
                .forEach(d -> result.add(new ContextSegment("structured:" + d.id(), String.valueOf(d.id()),
                        structuredSummary(d), d.fileName(), d.knowledgeVersion(), d.knowledgeType().name(),
                        d.knowledgeKey(), d.effectiveFrom(), d.effectiveTo(), false)));
        return result;
    }

    private static boolean isPriceQuery(String query) {
        String value = query == null ? "" : query;
        return List.of("价格", "多少钱", "收费", "费用", "原价", "优惠", "折扣", "打折", "几折", "办卡")
                .stream().anyMatch(value::contains);
    }

    private static String normalizeProduct(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[\\s、,，。:：?？]+", "");
    }

    private static String sourceNames(List<KnowledgeDocument> documents) {
        return documents.stream().map(d -> "《" + d.fileName() + "》v" + d.knowledgeVersion())
                .distinct().collect(Collectors.joining("、"));
    }

    private static String promotionDescription(KnowledgeDocument promotion) {
        return switch (promotion.discountType()) {
            case "PERCENT" -> promotion.discountValue().stripTrailingZeros().toPlainString() + "倍优惠";
            case "FIXED_AMOUNT" -> "减免" + promotion.discountValue().stripTrailingZeros().toPlainString() + "元";
            case "FIXED_PRICE" -> "优惠价" + promotion.discountValue().stripTrailingZeros().toPlainString() + "元";
            default -> "未识别的优惠形式";
        };
    }

    private static BigDecimal calculatePrice(BigDecimal base, KnowledgeDocument promotion) {
        return (switch (promotion.discountType()) {
            case "PERCENT" -> base.multiply(promotion.discountValue());
            case "FIXED_AMOUNT" -> base.subtract(promotion.discountValue()).max(BigDecimal.ZERO);
            case "FIXED_PRICE" -> promotion.discountValue();
            default -> base;
        }).setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private static String structuredSummary(KnowledgeDocument d) {
        StringBuilder text = new StringBuilder("结构化知识依据：").append(d.fileName()).append(" v").append(d.knowledgeVersion()).append("，类型=").append(d.knowledgeType());
        if (d.applicableProduct() != null) text.append("，适用项目=").append(d.applicableProduct());
        if (d.priceAmount() != null) text.append("，基础价格=").append(d.priceAmount()).append("元");
        if (d.discountType() != null) text.append("，优惠=").append(d.discountType()).append(" ").append(d.discountValue());
        if (d.effectiveFrom() != null) text.append("，生效=").append(d.effectiveFrom());
        if (d.effectiveTo() != null) text.append("，失效=").append(d.effectiveTo());
        if (d.promotionConditions() != null) text.append("，条件=").append(d.promotionConditions());
        return text.toString();
    }

    private static List<ContextSegment> localRerank(String query, List<ContextSegment> candidates) {
        if (candidates == null || candidates.size() < 2) return candidates == null ? List.of() : candidates;
        List<String> terms = queryTerms(query);
        if (terms.isEmpty()) return candidates;

        List<ScoredSegment> scored = new ArrayList<>();
        boolean hasEvidence = false;
        for (int i = 0; i < candidates.size(); i++) {
            ContextSegment candidate = candidates.get(i);
            String text = candidate.text() == null ? "" : candidate.text().toLowerCase(Locale.ROOT);
            int score = isQuestionList(candidate.text()) ? 0 : lexicalScore(text, terms);
            int matchedTerms = isQuestionList(candidate.text()) ? 0 : matchedTermCount(text, terms);
            hasEvidence |= score > 0;
            scored.add(new ScoredSegment(candidate, score, i, matchedTerms));
        }
        boolean evidence = hasEvidence;
        int minimumScore = terms.size() > 2 ? 4 : 1;
        int minimumMatches = terms.size() > 6 ? 4 : 1;
        return scored.stream()
                .filter(item -> !evidence || (item.score >= minimumScore && item.matchedTerms >= minimumMatches))
                .sorted(Comparator.comparingInt(ScoredSegment::score).reversed()
                        .thenComparingInt(ScoredSegment::originalOrder))
                .map(ScoredSegment::segment)
                .toList();
    }

    private static List<ContextSegment> filterLexicalEvidence(String query, List<ContextSegment> candidates) {
        if (candidates == null || candidates.size() < 2) return candidates == null ? List.of() : candidates;
        List<String> terms = queryTerms(query);
        if (terms.isEmpty()) return candidates;
        List<ScoredSegment> scored = candidates.stream()
                .map(candidate -> new ScoredSegment(candidate,
                        isQuestionList(candidate.text()) ? 0 : lexicalScore(candidate.text(), terms), 0,
                        isQuestionList(candidate.text()) ? 0 : matchedTermCount(candidate.text(), terms)))
                .toList();
        boolean hasEvidence = scored.stream().anyMatch(item -> item.score > 0);
        if (!hasEvidence) return candidates;
        int minimumScore = terms.size() > 2 ? 4 : 1;
        int minimumMatches = terms.size() > 6 ? 4 : 1;
        List<ContextSegment> filtered = scored.stream()
                .filter(item -> item.score >= minimumScore && item.matchedTerms >= minimumMatches)
                .map(ScoredSegment::segment)
                .toList();
        // Keep semantic/vector candidates when lexical evidence cannot identify
        // a strong match; otherwise a synonym-only query would incorrectly look
        // like an empty knowledge base.
        return filtered.isEmpty() ? candidates : filtered;
    }

    private static int lexicalScore(String text, List<String> terms) {
        String value = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return terms.stream().filter(value::contains).mapToInt(String::length).sum();
    }

    private static int matchedTermCount(String text, List<String> terms) {
        String value = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return (int) terms.stream().filter(value::contains).count();
    }

    private static boolean isQuestionList(String text) {
        if (text == null || text.isBlank()) return false;
        long questions = text.chars().filter(ch -> ch == '?' || ch == '？').count();
        if (questions < 2) return false;
        long declarative = text.chars().filter(ch -> ch == '。' || ch == ';' || ch == '；').count();
        return declarative == 0;
    }

    private static List<String> queryTerms(String query) {
        if (query == null || query.isBlank()) return List.of();
        Set<String> terms = new LinkedHashSet<>();
        for (String part : QUERY_PARTS.split(query.toLowerCase(Locale.ROOT))) {
            if (part.isBlank()) continue;
            boolean han = part.codePoints().allMatch(value -> value >= 0x4E00 && value <= 0x9FFF);
            if (han) {
                for (int i = 0; i + 1 < part.length(); i++) terms.add(part.substring(i, i + 2));
            } else if (part.length() >= 2) {
                terms.add(part);
            }
        }
        return List.copyOf(terms);
    }

    private record ScoredSegment(ContextSegment segment, int score, int originalOrder, int matchedTerms) { }

    private void recordDegradation(HybridRetriever.Result result, Consumer<AiProgressEvent> reporter,
                                   Set<String> failedRecallStages) {
        String headerValue = result.degradationHeaderValue();
        if (degradationContext != null) {
            degradationContext.applyDegradation(headerValue);
        }
        if (!result.degraded()) {
            return;
        }
        if (degradationMetrics != null) {
            degradationMetrics.recordDegraded(headerValue);
        }
        String stages = failedRecallStages.isEmpty()
                ? headerValue
                : String.join(",", failedRecallStages.stream().sorted().toList());
        emit(reporter, "recall_degraded", AiProgressEvent.Status.COMPLETED,
                "检索已降级，不可用阶段：" + stages, failedRecallStages.size());
    }

    private static void emit(Consumer<AiProgressEvent> progress, String stage,
                             AiProgressEvent.Status status, String message, Integer resultCount) {
        progress.accept(new AiProgressEvent("knowledge_search", stage, status, message, resultCount));
    }

    private static void emit(Consumer<AiProgressEvent> progress, String stage,
                             AiProgressEvent.Status status, String message, Integer resultCount,
                             List<com.gymmind.ai.harness.AiProgressDetail> details) {
        progress.accept(new AiProgressEvent("knowledge_search", stage, status, message, resultCount, details));
    }

    private List<ContextSegment> filterSearchable(CurrentActor actor, HybridRetriever.Result result) {
        return filterSearchable(actor, result, searchableDocuments(actor, result));
    }

    private List<ContextSegment> filterSearchable(CurrentActor actor, HybridRetriever.Result result,
                                                  Map<Long, KnowledgeDocument> searchable) {
        if (documents == null) return result.segments();
        return result.segments().stream().filter(segment -> {
            IndexedChunk chunk = result.chunksById().get(segment.id());
            return chunk == null || chunk.documentId() == null || searchable.containsKey(chunk.documentId());
        }).map(segment -> {
            IndexedChunk chunk = result.chunksById().get(segment.id());
            KnowledgeDocument document = chunk == null || chunk.documentId() == null
                    ? null : searchable.get(chunk.documentId());
            return document == null ? segment : new ContextSegment(segment.id(), segment.documentId(), segment.text(),
                    document.fileName(), document.knowledgeVersion(), document.knowledgeType().name(),
                    document.knowledgeKey(), document.effectiveFrom(), document.effectiveTo(), false);
        }).toList();
    }

    private Map<Long, KnowledgeDocument> searchableDocuments(CurrentActor actor, HybridRetriever.Result result) {
        if (documents == null) return null;
        Long tenantId = ActorAccess.tenantId(actor);
        var ids = result.chunksById().values().stream().map(com.gymmind.knowledge.infrastructure.search.IndexedChunk::documentId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, KnowledgeDocument> searchable = new HashMap<>();
        if (actor.isPlatformAdmin()) {
            documents.findByIdIn(ids).forEach(document -> searchable.put(document.id(), document));
        } else {
            if (tenantId != null) documents.findByTenantIdAndIdIn(tenantId, ids).forEach(document -> searchable.put(document.id(), document));
            documents.findPlatformDocuments().forEach(document -> searchable.put(document.id(), document));
        }
        List<KnowledgeDocument> indexedDocuments = new ArrayList<>(searchable.values());
        for (KnowledgeDocument document : indexedDocuments) {
            if (document.knowledgeKey() == null) continue;
            documents.findByKnowledgeKey(document.tenantId(), document.knowledgeKey())
                    .forEach(version -> searchable.put(version.id(), version));
        }
        searchable.values().removeIf(document -> document.status() != DocumentStatus.READY || !document.published()
        );
        Map<String, Integer> latest = searchable.values().stream().filter(d -> d.knowledgeKey() != null)
                .collect(Collectors.groupingBy(d -> (d.tenantId() == null ? "platform" : d.tenantId())
                                + "|" + d.knowledgeKey(),
                        Collectors.collectingAndThen(Collectors.maxBy(Comparator.comparingInt(KnowledgeDocument::knowledgeVersion)),
                                value -> value.map(KnowledgeDocument::knowledgeVersion).orElse(1))));
        searchable.values().removeIf(document -> document.knowledgeKey() != null
                && document.knowledgeVersion() < latest.getOrDefault((document.tenantId() == null ? "platform" : document.tenantId()) + "|" + document.knowledgeKey(), document.knowledgeVersion()));
        searchable.values().removeIf(document ->
                (document.visibility() == DocumentVisibility.PRIVATE_USER
                        && !Objects.equals(document.ownerUserId(), actor.userId()))
                        || (document.visibility() != DocumentVisibility.PRIVATE_USER
                        && !ActorAccess.canManageTenant(actor)
                        && document.tenantId() != null
                        && document.visibility() != DocumentVisibility.TENANT)
                        || (!actor.isPlatformAdmin()
                        && document.effectiveAudienceRoles().stream().noneMatch(actor.roles()::contains)));
        return searchable;
    }

    private static Set<String> authorizedChunkIds(HybridRetriever.Result result,
                                                    Map<Long, KnowledgeDocument> searchable) {
        if (searchable == null) {
            return result.chunksById().keySet();
        }
        if (searchable.isEmpty() && result.chunksById().values().stream().allMatch(chunk -> chunk.documentId() == null)) {
            return result.chunksById().keySet();
        }
        return result.chunksById().values().stream()
                .filter(chunk -> chunk.documentId() == null || searchable.containsKey(chunk.documentId()))
                .filter(chunk -> {
                    if (chunk.documentId() == null || searchable == null) {
                        return true;
                    }
                    KnowledgeDocument document = searchable.get(chunk.documentId());
                    return document == null || chunk.matchesIndexGeneration(document.indexGeneration());
                })
                .map(IndexedChunk::id)
                .collect(Collectors.toSet());
    }

    private static int authorizedCount(List<IndexedChunk> chunks, Set<String> authorizedIds) {
        return (int) chunks.stream().filter(chunk -> authorizedIds.contains(chunk.id())).count();
    }

    private static List<com.gymmind.ai.harness.AiProgressDetail> chunkDetails(
            List<IndexedChunk> chunks, Set<String> authorizedIds, Map<Long, KnowledgeDocument> documents,
            String title) {
        List<com.gymmind.ai.harness.AiProgressDetail> details = new ArrayList<>();
        for (int i = 0; i < chunks.size() && details.size() < MAX_PROGRESS_DETAILS; i++) {
            IndexedChunk chunk = chunks.get(i);
            if (!authorizedIds.contains(chunk.id())) continue;
            details.add(detail("CHUNK", title, chunk, documents, i + 1));
        }
        return details;
    }

    private static List<com.gymmind.ai.harness.AiProgressDetail> segmentDetails(
            List<ContextSegment> segments, Map<String, IndexedChunk> chunks,
            Map<Long, KnowledgeDocument> documents, String title) {
        List<com.gymmind.ai.harness.AiProgressDetail> details = new ArrayList<>();
        for (int i = 0; i < segments.size() && details.size() < MAX_PROGRESS_DETAILS; i++) {
            IndexedChunk chunk = chunks.get(segments.get(i).id());
            if (chunk != null) details.add(detail("CHUNK", title, chunk, documents, i + 1));
            else details.add(new com.gymmind.ai.harness.AiProgressDetail("CHUNK", title,
                    segments.get(i).documentId(), segments.get(i).id(), null,
                    excerpt(segments.get(i).text()), i + 1));
        }
        return details;
    }

    private static com.gymmind.ai.harness.AiProgressDetail detail(String kind, String title,
                                                                    IndexedChunk chunk,
                                                                    Map<Long, KnowledgeDocument> documents,
                                                                    int rank) {
        KnowledgeDocument document = chunk.documentId() == null || documents == null ? null : documents.get(chunk.documentId());
        return new com.gymmind.ai.harness.AiProgressDetail(kind, title,
                chunk.documentId() == null ? null : String.valueOf(chunk.documentId()), chunk.id(),
                document == null ? null : document.fileName(), excerpt(chunk.text()), rank);
    }

    private static String excerpt(String text) {
        if (text == null) return "";
        return text.length() <= MAX_PROGRESS_EXCERPT ? text : text.substring(0, MAX_PROGRESS_EXCERPT);
    }

    private static List<com.gymmind.ai.harness.AiProgressDetail> permissionDetails(CurrentActor actor,
                                                                                      List<Long> scopes) {
        String roles = actor.roles().stream().map(DefaultKnowledgeSearchService::roleLabel).collect(Collectors.joining("、"));
        String tenant = actor.tenantId() == null ? "平台上下文" : "健身房 " + actor.tenantId();
        String scope = scopes.stream().map(id -> id == KnowledgeIndexingService.PLATFORM_INDEX_TENANT_ID
                        ? "平台公共文档" : "健身房 " + id + " 文档")
                .collect(Collectors.joining("、"));
        return List.of(
                new com.gymmind.ai.harness.AiProgressDetail("ACTOR", "当前身份", null, null, null,
                        excerpt("角色：" + roles + "；租户：" + tenant), null),
                new com.gymmind.ai.harness.AiProgressDetail("SCOPE", "检索范围", null, null, null,
                        excerpt(scope), null));
    }

    private static String roleLabel(RoleCode role) {
        return switch (role) {
            case PLATFORM_ADMIN -> "平台管理员";
            case GYM_ADMIN -> "健身房管理员";
            case COACH -> "教练";
            case MEMBER -> "会员";
        };
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
