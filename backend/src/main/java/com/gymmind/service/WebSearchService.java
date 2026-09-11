package com.gymmind.service;

import com.gymmind.dto.search.SearchResult;
import com.gymmind.dto.search.SearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class WebSearchService {

    // 模拟搜索服务，实际应该集成真实的搜索API
    public SearchResponse search(String query) {
        log.info("Searching for: {}", query);

        List<SearchResult> results = new ArrayList<>();

        // 模拟搜索结果
        results.add(SearchResult.builder()
                .title("How to Build Muscle Mass - Complete Guide")
                .url("https://example.com/muscle-building-guide")
                .snippet("Learn the best exercises, nutrition tips, and training strategies to build muscle mass effectively...")
                .source("Fitness Expert Blog")
                .publishedDate("2024-01-15")
                .relevanceScore(0.95)
                .build());

        results.add(SearchResult.builder()
                .title("Protein Intake for Muscle Growth")
                .url("https://example.com/protein-intake")
                .snippet("Scientific guide on optimal protein intake for muscle growth, including timing and sources...")
                .source("Nutrition Science Journal")
                .publishedDate("2024-02-20")
                .relevanceScore(0.88)
                .build());

        results.add(SearchResult.builder()
                .title("Best Workout Splits for Beginners")
                .url("https://example.com/workout-splits")
                .snippet("Discover the most effective workout split routines for beginners to maximize gains...")
                .source("Gym Training Hub")
                .publishedDate("2024-03-10")
                .relevanceScore(0.82)
                .build());

        return SearchResponse.builder()
                .results(results)
                .totalResults(results.size())
                .query(query)
                .build();
    }
}
