package com.gymmind.ai.application;

import com.gymmind.ai.domain.ContextSegment;

import java.util.Locale;

public record SourceCitation(String chunkId, String documentId, String excerpt, String documentName,
                             Integer documentVersion, String knowledgeType, String knowledgeKey,
                             java.time.LocalDateTime effectiveFrom, java.time.LocalDateTime effectiveTo) {
    private static final int MAX_EXCERPT_LENGTH = 360;
    public SourceCitation(String chunkId, String documentId, String excerpt) {
        this(chunkId, documentId, excerpt, null, null, null, null, null, null);
    }

    public static SourceCitation from(ContextSegment segment, String query) {
        if (segment == null) return new SourceCitation(null, null, "", null, null, null, null, null, null);
        return new SourceCitation(segment.id(), segment.documentId(), compactExcerpt(segment.text(), query),
                segment.documentName(), segment.documentVersion(), segment.knowledgeType(), segment.knowledgeKey(),
                segment.effectiveFrom(), segment.effectiveTo());
    }

    static String compactExcerpt(String text, String query) {
        if (text == null || text.isBlank()) return "";
        if (text.length() <= MAX_EXCERPT_LENGTH) return text;
        String value = text.toLowerCase(Locale.ROOT);
        int match = findMatch(value, query);
        if (match < 0) return text.substring(0, MAX_EXCERPT_LENGTH) + "...";
        int start = Math.max(0, match - MAX_EXCERPT_LENGTH / 2);
        int end = Math.min(text.length(), start + MAX_EXCERPT_LENGTH);
        if (end - start < MAX_EXCERPT_LENGTH) start = Math.max(0, end - MAX_EXCERPT_LENGTH);
        String excerpt = text.substring(start, end).trim();
        return (start > 0 ? "..." : "") + excerpt + (end < text.length() ? "..." : "");
    }

    private static int findMatch(String text, String query) {
        if (query == null || query.isBlank()) return -1;
        String normalized = query.toLowerCase(Locale.ROOT).trim();
        int exact = text.indexOf(normalized);
        if (exact >= 0) return exact;
        for (int length = Math.min(4, normalized.length()); length >= 2; length--) {
            for (int offset = 0; offset + length <= normalized.length(); offset++) {
                String term = normalized.substring(offset, offset + length);
                if (term.codePoints().allMatch(c -> c >= 0x4E00 && c <= 0x9FFF)) {
                    int found = text.indexOf(term);
                    if (found >= 0) return found;
                }
            }
        }
        return -1;
    }
}
