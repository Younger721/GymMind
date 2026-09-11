package com.gymmind.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.gymmind.dto.rag.RetrievalResult;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.SearchResults;
import io.milvus.param.R;
import io.milvus.param.dml.SearchParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HybridRetrievalService {

    private final MilvusServiceClient milvusClient;
    private final ElasticsearchClient elasticsearchClient;
    private final EmbeddingService embeddingService;

    @Value("${milvus.collection.knowledge}")
    private String milvusCollection;

    @Value("${elasticsearch.index.knowledge}")
    private String esIndex;

    @Value("${rag.retrieval.top-k-vector:10}")
    private int topKVector;

    @Value("${rag.retrieval.top-k-keyword:10}")
    private int topKKeyword;

    @Value("${rag.retrieval.final-top-k:5}")
    private int finalTopK;

    public List<RetrievalResult> hybridSearch(Long userId, String query) {
        log.info("Hybrid search for userId={}, query={}", userId, query);

        // Generate query embedding
        float[] queryEmbedding = embeddingService.generateEmbedding(query);

        // Vector search (Milvus)
        List<RetrievalResult> vectorResults = vectorSearch(userId, queryEmbedding);

        // Keyword search (Elasticsearch)
        List<RetrievalResult> keywordResults = keywordSearch(userId, query);

        // RRF Fusion
        List<RetrievalResult> fusedResults = reciprocalRankFusion(vectorResults, keywordResults);

        // Return top-k
        return fusedResults.stream()
                .limit(finalTopK)
                .collect(Collectors.toList());
    }

    private List<RetrievalResult> vectorSearch(Long userId, float[] embedding) {
        try {
            String expr = String.format("user_id == %d", userId);

            SearchParam searchParam = SearchParam.newBuilder()
                    .withCollectionName(milvusCollection)
                    .withMetricType(io.milvus.param.MetricType.COSINE)
                    .withOutFields(Arrays.asList("user_id", "document_id", "chunk_index"))
                    .withTopK(topKVector)
                    .withVectors(Collections.singletonList(embedding))
                    .withVectorFieldName("embedding")
                    .withExpr(expr)
                    .build();

            R<SearchResults> response = milvusClient.search(searchParam);

            if (response.getStatus() != R.Status.Success.getCode()) {
                log.error("Milvus search failed: {}", response.getMessage());
                return Collections.emptyList();
            }

            List<RetrievalResult> results = new ArrayList<>();
            SearchResults searchResults = response.getData();

            if (searchResults.getResults().getFieldsDataList().isEmpty()) {
                return results;
            }

            for (int i = 0; i < searchResults.getResults().getScoresList().size(); i++) {
                float score = searchResults.getResults().getScores(i);
                // Extract metadata from fields
                Long documentId = extractDocumentId(searchResults, i);
                Integer chunkIndex = extractChunkIndex(searchResults, i);

                RetrievalResult result = RetrievalResult.builder()
                        .documentId(documentId)
                        .chunkIndex(chunkIndex)
                        .score((double) score)
                        .source("vector")
                        .build();

                results.add(result);
            }

            log.info("Vector search returned {} results", results.size());
            return results;

        } catch (Exception e) {
            log.error("Vector search failed", e);
            return Collections.emptyList();
        }
    }

    private List<RetrievalResult> keywordSearch(Long userId, String query) {
        try {
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(esIndex)
                    .query(q -> q
                            .bool(b -> b
                                    .must(m -> m
                                            .term(t -> t
                                                    .field("user_id")
                                                    .value(userId)
                                            )
                                    )
                                    .must(m -> m
                                            .match(ma -> ma
                                                    .field("chunk_text")
                                                    .query(query)
                                            )
                                    )
                            )
                    )
                    .size(topKKeyword)
            );

            SearchResponse<Map> response = elasticsearchClient.search(searchRequest, Map.class);

            List<RetrievalResult> results = new ArrayList<>();

            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source == null) continue;

                Long documentId = ((Number) source.get("document_id")).longValue();
                Integer chunkIndex = ((Number) source.get("chunk_index")).intValue();
                Double score = hit.score();

                RetrievalResult result = RetrievalResult.builder()
                        .documentId(documentId)
                        .chunkIndex(chunkIndex)
                        .chunkText((String) source.get("chunk_text"))
                        .documentName((String) source.get("document_name"))
                        .category((String) source.get("category"))
                        .sourceType((String) source.get("source_type"))
                        .sourceUrl((String) source.get("source_url"))
                        .score(score)
                        .source("keyword")
                        .build();

                results.add(result);
            }

            log.info("Keyword search returned {} results", results.size());
            return results;

        } catch (IOException e) {
            log.error("Keyword search failed", e);
            return Collections.emptyList();
        }
    }

    private List<RetrievalResult> reciprocalRankFusion(List<RetrievalResult> vectorResults,
                                                       List<RetrievalResult> keywordResults) {
        Map<String, RetrievalResult> fusedMap = new HashMap<>();
        Map<String, Double> rrfScores = new HashMap<>();
        int k = 60; // RRF constant

        // Process vector results
        for (int i = 0; i < vectorResults.size(); i++) {
            RetrievalResult result = vectorResults.get(i);
            String key = result.getDocumentId() + "_" + result.getChunkIndex();
            double rrfScore = 1.0 / (k + i + 1);

            rrfScores.put(key, rrfScores.getOrDefault(key, 0.0) + rrfScore);
            fusedMap.putIfAbsent(key, result);
        }

        // Process keyword results
        for (int i = 0; i < keywordResults.size(); i++) {
            RetrievalResult result = keywordResults.get(i);
            String key = result.getDocumentId() + "_" + result.getChunkIndex();
            double rrfScore = 1.0 / (k + i + 1);

            rrfScores.put(key, rrfScores.getOrDefault(key, 0.0) + rrfScore);
            if (!fusedMap.containsKey(key)) {
                fusedMap.put(key, result);
            } else {
                // Merge metadata from keyword result
                RetrievalResult existing = fusedMap.get(key);
                if (existing.getChunkText() == null) {
                    existing.setChunkText(result.getChunkText());
                    existing.setDocumentName(result.getDocumentName());
                    existing.setCategory(result.getCategory());
                    existing.setSourceType(result.getSourceType());
                    existing.setSourceUrl(result.getSourceUrl());
                }
            }
        }

        // Build final sorted list
        List<RetrievalResult> fusedResults = new ArrayList<>();
        for (Map.Entry<String, Double> entry : rrfScores.entrySet()) {
            RetrievalResult result = fusedMap.get(entry.getKey());
            result.setScore(entry.getValue());
            result.setSource("hybrid");
            fusedResults.add(result);
        }

        fusedResults.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        log.info("RRF fusion produced {} results", fusedResults.size());
        return fusedResults;
    }

    private Long extractDocumentId(SearchResults searchResults, int index) {
        try {
            return searchResults.getResults().getFieldsData(1).getScalars().getLongData().getData(index);
        } catch (Exception e) {
            return 0L;
        }
    }

    private Integer extractChunkIndex(SearchResults searchResults, int index) {
        try {
            return (int) searchResults.getResults().getFieldsData(2).getScalars().getLongData().getData(index);
        } catch (Exception e) {
            return 0;
        }
    }
}
