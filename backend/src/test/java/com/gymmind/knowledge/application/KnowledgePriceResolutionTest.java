package com.gymmind.knowledge.application;

import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.domain.EmbeddingGateway;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import com.gymmind.knowledge.domain.model.KnowledgeType;
import com.gymmind.knowledge.infrastructure.search.IndexedChunk;
import com.gymmind.knowledge.infrastructure.search.KeywordIndexPort;
import com.gymmind.knowledge.infrastructure.search.VectorIndexPort;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.application.TenantQuotaService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgePriceResolutionTest {
    private static final CurrentActor ADMIN = new CurrentActor(11L, 7L, Set.of(RoleCode.GYM_ADMIN),
            Set.of("knowledge:read"), 0, "test");

    @Test
    void latestPriceAndSingleActivePromotionProduceDeterministicQuoteAndSources() {
        var baseV1 = price(101L, "base-month", 1, "月卡旧价", "100");
        var baseV2 = price(102L, "base-month", 2, "月卡新价", "90");
        var sale = promotion(103L, "summer-sale", "夏季活动", "0.90", 10);
        var result = service(List.of(baseV1, baseV2, sale), List.of()).search(ADMIN, "月卡多少钱？");

        assertThat(result).filteredOn(ContextSegment::authoritativeAnswer).singleElement().satisfies(segment -> {
            assertThat(segment.text()).contains("81元").contains("月卡新价").contains("夏季活动");
            assertThat(segment.knowledgeType()).isEqualTo("PRICE_RESULT");
        });
        assertThat(result).extracting(ContextSegment::documentId).contains("102", "103").doesNotContain("101");
    }

    @Test
    void conflictingHighestPriorityPromotionsRequireManualConfirmation() {
        var base = price(201L, "base", 1, "月卡价格", "90");
        var saleA = promotion(202L, "sale-a", "活动甲", "0.90", 10);
        var saleB = promotion(203L, "sale-b", "活动乙", "0.80", 10);

        var result = service(List.of(base, saleA, saleB), List.of()).search(ADMIN, "月卡多少钱？");

        assertThat(result).anySatisfy(segment -> assertThat(segment.text())
                .contains("需人工确认").contains("活动甲").contains("活动乙"))
                .noneMatch(segment -> segment.text().contains("应付价格"));
    }

    @Test
    void multipleMatchedProductsDoNotApplyAProductSpecificDiscount() {
        var month = price(301L, "base-month", 1, "月卡价格", "90");
        var annual = price(302L, "base-year", 1, "年卡价格", "900");
        var sale = promotion(303L, "sale-month", "月卡活动", "0.90", 10);

        var result = service(List.of(month, annual, sale), List.of()).search(ADMIN, "月卡和年卡办卡多少钱？");

        assertThat(result).anySatisfy(segment -> assertThat(segment.text())
                .contains("请先确认卡种").contains("月卡").contains("年卡"))
                .noneMatch(segment -> segment.text().contains("应付价格"));
    }

    @Test
    void staleIndexedVersionIsExcludedWhenLatestVersionWasNotRecalled() {
        var old = price(401L, "price-topic", 1, "旧价格", "100");
        var current = price(402L, "price-topic", 2, "当前价格", "90");
        var keyword = mock(KeywordIndexPort.class);
        var vector = mock(VectorIndexPort.class);
        var embedding = mock(EmbeddingGateway.class);
        var documents = mock(KnowledgeDocumentRepository.class);
        when(embedding.embed(any())).thenReturn(List.of(new float[1024]));
        when(keyword.search(any(), eq(7L), eq(11L))).thenReturn(List.of(
                new IndexedChunk("old-chunk", 7L, 401L, null, "旧价格 月卡 100 元", new float[1024], "price-topic", 1)));
        when(vector.search(any(), eq(7L), eq(11L))).thenReturn(List.of());
        when(documents.findByTenantIdAndIdIn(eq(7L), any())).thenReturn(List.of(old));
        when(documents.findByKnowledgeKey(7L, "price-topic")).thenReturn(List.of(old, current));
        when(documents.findPlatformDocuments()).thenReturn(List.of());

        var result = new DefaultKnowledgeSearchService(keyword, vector, embedding, null,
                mock(TenantQuotaService.class), documents).search(ADMIN, "旧价格 月卡 100 元");

        assertThat(result).extracting(ContextSegment::id).doesNotContain("old-chunk");
    }

    private static DefaultKnowledgeSearchService service(List<KnowledgeDocument> rules, List<IndexedChunk> hits) {
        var keyword = mock(KeywordIndexPort.class);
        var vector = mock(VectorIndexPort.class);
        var embedding = mock(EmbeddingGateway.class);
        var documents = mock(KnowledgeDocumentRepository.class);
        when(embedding.embed(any())).thenReturn(List.of(new float[1024]));
        when(keyword.search(any(), eq(7L), eq(11L))).thenReturn(hits);
        when(vector.search(any(), eq(7L), eq(11L))).thenReturn(List.of());
        when(documents.findByTenantIdAndIdIn(eq(7L), any())).thenReturn(List.of());
        when(documents.findPriceRules(7L)).thenReturn(rules);
        when(documents.findPlatformDocuments()).thenReturn(List.of());
        return new DefaultKnowledgeSearchService(keyword, vector, embedding, null,
                mock(TenantQuotaService.class), documents);
    }

    private static KnowledgeDocument price(long id, String key, int version, String name, String amount) {
        var document = KnowledgeDocument.create(7L, null, name, "text/plain", DocumentVisibility.TENANT, "test");
        document.assignId(id);
        document.assignVersion(key, version);
        document.configureKnowledge(KnowledgeType.BASE_PRICE, name.contains("年卡") ? "年卡" : "月卡",
                new BigDecimal(amount), null, null, null, null, null, 0);
        document.markParsing(); document.markReady(); document.publish();
        return document;
    }

    private static KnowledgeDocument promotion(long id, String key, String name, String multiplier, int priority) {
        var document = KnowledgeDocument.create(7L, null, name, "text/plain", DocumentVisibility.TENANT, "test");
        document.assignId(id);
        document.assignVersion(key, 1);
        LocalDateTime now = LocalDateTime.now();
        document.configureKnowledge(KnowledgeType.PROMOTION_RULE, "月卡", null, "PERCENT",
                new BigDecimal(multiplier), now.minusDays(1), now.plusDays(10), null, priority);
        document.markParsing(); document.markReady(); document.publish();
        return document;
    }
}
