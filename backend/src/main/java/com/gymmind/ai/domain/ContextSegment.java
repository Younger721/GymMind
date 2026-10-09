package com.gymmind.ai.domain;

public record ContextSegment(String id, String documentId, String text,
                             String documentName, Integer documentVersion, String knowledgeType,
                             String knowledgeKey, java.time.LocalDateTime effectiveFrom,
                             java.time.LocalDateTime effectiveTo, boolean authoritativeAnswer) {
    public ContextSegment(String id, String documentId, String text) {
        this(id, documentId, text, null, null, null, null, null, null, false);
    }

    public ContextSegment(String id, String documentId, String text,
                          String documentName, Integer documentVersion, String knowledgeType) {
        this(id, documentId, text, documentName, documentVersion, knowledgeType, null, null, null, false);
    }
}
