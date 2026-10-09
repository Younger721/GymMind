package com.gymmind.ai.domain;

public record ContextSegment(String id, String documentId, String text,
                             String documentName, Integer documentVersion, String knowledgeType) {
    public ContextSegment(String id, String documentId, String text) {
        this(id, documentId, text, null, null, null);
    }
}
