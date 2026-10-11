package com.gymmind.knowledge.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 知识库检索超参、词面过滤阈值、query 向量缓存与 rerank 重试（S6 配置外置）。 */
@Component
public class KnowledgeSearchProperties {

    private final int recallTopK;
    private final int fusionTopK;
    private final int maxSegmentsPerDocument;
    private final int contextBudgetChars;
    private final boolean queryEmbeddingCacheEnabled;
    private final long queryEmbeddingCacheTtlSeconds;
    private final int queryEmbeddingCacheMaxEntries;
    private final int rerankMaxAttempts;
    private final long rerankRetryBaseMs;
    private final int lexicalLongQueryTermThreshold;
    private final int lexicalLongQueryMinScore;
    private final int lexicalVeryLongQueryTermThreshold;
    private final int lexicalVeryLongQueryMinMatches;

    public KnowledgeSearchProperties(
            @Value("${gymmind.search.recall-top-k:30}") int recallTopK,
            @Value("${gymmind.search.fusion-top-k:20}") int fusionTopK,
            @Value("${gymmind.search.max-segments-per-document:3}") int maxSegmentsPerDocument,
            @Value("${gymmind.search.context-budget-chars:12000}") int contextBudgetChars,
            @Value("${gymmind.search.query-embedding-cache.enabled:true}") boolean queryEmbeddingCacheEnabled,
            @Value("${gymmind.search.query-embedding-cache.ttl-seconds:300}") long queryEmbeddingCacheTtlSeconds,
            @Value("${gymmind.search.query-embedding-cache.max-entries:512}") int queryEmbeddingCacheMaxEntries,
            @Value("${gymmind.search.rerank.max-attempts:2}") int rerankMaxAttempts,
            @Value("${gymmind.search.rerank.retry-base-ms:200}") long rerankRetryBaseMs,
            @Value("${gymmind.search.lexical.long-query-term-threshold:2}") int lexicalLongQueryTermThreshold,
            @Value("${gymmind.search.lexical.long-query-min-score:4}") int lexicalLongQueryMinScore,
            @Value("${gymmind.search.lexical.very-long-query-term-threshold:6}") int lexicalVeryLongQueryTermThreshold,
            @Value("${gymmind.search.lexical.very-long-query-min-matches:4}") int lexicalVeryLongQueryMinMatches) {
        if (recallTopK < 1 || fusionTopK < 1) {
            throw new IllegalArgumentException("recallTopK and fusionTopK must be positive");
        }
        if (maxSegmentsPerDocument < 1 || contextBudgetChars < 1) {
            throw new IllegalArgumentException("maxSegmentsPerDocument and contextBudgetChars must be positive");
        }
        if (queryEmbeddingCacheMaxEntries < 1) {
            throw new IllegalArgumentException("queryEmbeddingCacheMaxEntries must be positive");
        }
        if (rerankMaxAttempts < 1) {
            throw new IllegalArgumentException("rerankMaxAttempts must be positive");
        }
        if (rerankRetryBaseMs < 0) {
            throw new IllegalArgumentException("rerankRetryBaseMs must be non-negative");
        }
        this.recallTopK = recallTopK;
        this.fusionTopK = fusionTopK;
        this.maxSegmentsPerDocument = maxSegmentsPerDocument;
        this.contextBudgetChars = contextBudgetChars;
        this.queryEmbeddingCacheEnabled = queryEmbeddingCacheEnabled;
        this.queryEmbeddingCacheTtlSeconds = queryEmbeddingCacheTtlSeconds;
        this.queryEmbeddingCacheMaxEntries = queryEmbeddingCacheMaxEntries;
        this.rerankMaxAttempts = rerankMaxAttempts;
        this.rerankRetryBaseMs = rerankRetryBaseMs;
        this.lexicalLongQueryTermThreshold = lexicalLongQueryTermThreshold;
        this.lexicalLongQueryMinScore = lexicalLongQueryMinScore;
        this.lexicalVeryLongQueryTermThreshold = lexicalVeryLongQueryTermThreshold;
        this.lexicalVeryLongQueryMinMatches = lexicalVeryLongQueryMinMatches;
    }

    /** 单测与非 Spring 构造链使用的默认值（与 application.yml 默认一致）。 */
    public static KnowledgeSearchProperties defaults() {
        return new KnowledgeSearchProperties(
                30, 20, 3, 12000,
                true, 300, 512,
                2, 200,
                2, 4, 6, 4);
    }

    public int recallTopK() {
        return recallTopK;
    }

    public int fusionTopK() {
        return fusionTopK;
    }

    public int maxSegmentsPerDocument() {
        return maxSegmentsPerDocument;
    }

    public int contextBudgetChars() {
        return contextBudgetChars;
    }

    public boolean queryEmbeddingCacheEnabled() {
        return queryEmbeddingCacheEnabled;
    }

    public long queryEmbeddingCacheTtlSeconds() {
        return queryEmbeddingCacheTtlSeconds;
    }

    public int queryEmbeddingCacheMaxEntries() {
        return queryEmbeddingCacheMaxEntries;
    }

    public int rerankMaxAttempts() {
        return rerankMaxAttempts;
    }

    /** 第 n 次重试前等待毫秒数：base * 2^(n-1)。 */
    public long rerankRetryDelayMs(int attemptIndex) {
        if (attemptIndex < 1) {
            return rerankRetryBaseMs;
        }
        return rerankRetryBaseMs << (attemptIndex - 1);
    }

    public int lexicalMinimumScore(int termCount) {
        return termCount > lexicalLongQueryTermThreshold ? lexicalLongQueryMinScore : 1;
    }

    public int lexicalMinimumMatches(int termCount) {
        return termCount > lexicalVeryLongQueryTermThreshold ? lexicalVeryLongQueryMinMatches : 1;
    }
}
