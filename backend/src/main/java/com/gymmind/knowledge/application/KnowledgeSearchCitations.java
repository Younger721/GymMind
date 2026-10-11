package com.gymmind.knowledge.application;

import com.gymmind.ai.application.SourceCitation;
import com.gymmind.ai.domain.ContextSegment;

import java.util.List;
import java.util.Objects;

/** 将 {@link KnowledgeSearchService} 返回的片段转为引用，与 Search API 顺序一致。 */
public final class KnowledgeSearchCitations {

    private KnowledgeSearchCitations() {
    }

    public static boolean isKnowledgeSegment(ContextSegment segment) {
        if (segment == null || segment.id() == null || segment.authoritativeAnswer()) {
            return false;
        }
        String documentId = segment.documentId();
        return !"TOOL_RESULT".equals(documentId)
                && !"TOOL_ERROR".equals(documentId)
                && (documentId == null || !documentId.startsWith("tool:"));
    }

    public static List<String> chunkIdsInOrder(List<ContextSegment> knowledgeSegments) {
        if (knowledgeSegments == null || knowledgeSegments.isEmpty()) {
            return List.of();
        }
        return knowledgeSegments.stream()
                .filter(KnowledgeSearchCitations::isKnowledgeSegment)
                .map(ContextSegment::id)
                .filter(Objects::nonNull)
                .toList();
    }

    public static List<SourceCitation> toCitations(List<ContextSegment> knowledgeSegments, String query) {
        if (knowledgeSegments == null || knowledgeSegments.isEmpty()) {
            return List.of();
        }
        return knowledgeSegments.stream()
                .filter(KnowledgeSearchCitations::isKnowledgeSegment)
                .map(segment -> SourceCitation.from(segment, query))
                .toList();
    }
}
