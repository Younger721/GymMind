package com.gymmind.ai.application;

import com.gymmind.ai.domain.ContextSegment;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record SourceCitation(String chunkId, String documentId, String excerpt, String documentName,
                             Integer documentVersion, String knowledgeType, String knowledgeKey,
                             java.time.LocalDateTime effectiveFrom, java.time.LocalDateTime effectiveTo,
                             String chunkLabel) {
    private static final int MAX_EXCERPT_LENGTH = 360;
    /** 片段文本开头的条款面包屑，如「【第三章 会员卡管理 · 第12条 退卡规定】」 */
    private static final Pattern BREADCRUMB = Pattern.compile("^\\s*【([^】]{1,80})】");

    public SourceCitation(String chunkId, String documentId, String excerpt) {
        this(chunkId, documentId, excerpt, null, null, null, null, null, null, null);
    }

    /** 兼容既有 9 参调用点：条款标签缺省为空 */
    public SourceCitation(String chunkId, String documentId, String excerpt, String documentName,
                          Integer documentVersion, String knowledgeType, String knowledgeKey,
                          java.time.LocalDateTime effectiveFrom, java.time.LocalDateTime effectiveTo) {
        this(chunkId, documentId, excerpt, documentName, documentVersion, knowledgeType, knowledgeKey,
                effectiveFrom, effectiveTo, null);
    }

    public static SourceCitation from(ContextSegment segment, String query) {
        if (segment == null) return new SourceCitation(null, null, "", null, null, null, null, null, null, null);
        String text = segment.text();
        return new SourceCitation(segment.id(), segment.documentId(), compactExcerpt(text, query),
                segment.documentName(), segment.documentVersion(), segment.knowledgeType(), segment.knowledgeKey(),
                segment.effectiveFrom(), segment.effectiveTo(), chunkLabel(text));
    }

    /** 从片段文本开头提取条款面包屑，供引用展示精确到「第X条」；无面包屑时返回 null */
    public static String chunkLabel(String text) {
        if (text == null || text.isBlank()) return null;
        Matcher matcher = BREADCRUMB.matcher(text.trim());
        return matcher.find() ? matcher.group(1).trim() : null;
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
