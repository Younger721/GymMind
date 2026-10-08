package com.gymmind.knowledge.infrastructure.search;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "gymmind.search.keyword-backend", havingValue = "memory")
public class InMemoryKeywordIndex implements KeywordIndexPort {
    private static final Pattern PARTS = Pattern.compile("[^\\p{IsHan}A-Za-z0-9]+");
    private final Map<String, IndexedChunk> data = new HashMap<>();

    public void upsert(IndexedChunk chunk) {
        data.put(chunk.id(), chunk);
    }

    public List<IndexedChunk> search(String query, Long tenantId) {
        List<String> terms = terms(query);
        return data.values().stream()
                .filter(chunk -> tenantId.equals(chunk.tenantId()))
                .map(chunk -> new Scored(chunk, score(chunk.text(), terms)))
                .filter(item -> terms.isEmpty() || item.score() > 0)
                .sorted(Comparator.comparingInt(Scored::score).reversed()
                        .thenComparing(item -> item.chunk().id()))
                .map(Scored::chunk)
                .toList();
    }

    public List<IndexedChunk> search(String query, Long tenantId, Long userId) {
        return search(query, tenantId).stream().filter(chunk -> chunk.visibleTo(userId)).toList();
    }

    public void delete(String id, Long tenantId) {
        data.computeIfPresent(id, (key, chunk) -> tenantId.equals(chunk.tenantId()) ? null : chunk);
    }

    public void deleteDocument(Long tenantId, Long documentId) {
        data.entrySet().removeIf(e -> tenantId.equals(e.getValue().tenantId()) && documentId.equals(e.getValue().documentId()));
    }

    private static int score(String text, List<String> terms) {
        if (terms.isEmpty()) return 1;
        String value = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return terms.stream().filter(value::contains).mapToInt(String::length).sum();
    }

    private static List<String> terms(String query) {
        if (query == null || query.isBlank()) return List.of();
        Set<String> result = new LinkedHashSet<>();
        for (String part : PARTS.split(query.toLowerCase(Locale.ROOT))) {
            if (part.isBlank()) continue;
            boolean han = part.codePoints().allMatch(value -> value >= 0x4E00 && value <= 0x9FFF);
            if (han) {
                for (int i = 0; i + 1 < part.length(); i++) result.add(part.substring(i, i + 2));
            } else if (part.length() >= 2) {
                result.add(part);
            }
        }
        return new ArrayList<>(result);
    }

    private record Scored(IndexedChunk chunk, int score) { }
}
