package com.gymmind.knowledge.infrastructure.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** 检索 query 的向量缓存（按规范化 query 文本，TTL + 容量上限）。 */
public final class SearchQueryEmbeddingCache {

    private record Entry(float[] vector, long expiresAtMs) { }

    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final long ttlMs;
    private final int maxEntries;

    public SearchQueryEmbeddingCache(long ttlSeconds, int maxEntries) {
        if (ttlSeconds < 1) {
            throw new IllegalArgumentException("ttlSeconds must be positive");
        }
        if (maxEntries < 1) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        this.ttlMs = ttlSeconds * 1000L;
        this.maxEntries = maxEntries;
    }

    public float[] getOrCompute(String query, Supplier<float[]> compute) {
        if (query == null || query.isBlank()) {
            return compute.get();
        }
        String key = query.strip();
        long now = System.currentTimeMillis();
        Entry hit = entries.get(key);
        if (hit != null && hit.expiresAtMs > now) {
            return hit.vector;
        }
        float[] vector = compute.get();
        if (vector == null) {
            return null;
        }
        evictExpired(now);
        if (entries.size() >= maxEntries) {
            evictOldest(now);
        }
        entries.put(key, new Entry(vector, now + ttlMs));
        return vector;
    }

    private void evictExpired(long now) {
        entries.entrySet().removeIf(entry -> entry.getValue().expiresAtMs <= now);
    }

    private void evictOldest(long now) {
        evictExpired(now);
        if (entries.size() < maxEntries) {
            return;
        }
        List<String> oldest = entries.entrySet().stream()
                .sorted(Comparator.comparingLong(entry -> entry.getValue().expiresAtMs))
                .limit(entries.size() - maxEntries + 1L)
                .map(java.util.Map.Entry::getKey)
                .toList();
        for (String key : new ArrayList<>(oldest)) {
            entries.remove(key);
        }
    }
}
