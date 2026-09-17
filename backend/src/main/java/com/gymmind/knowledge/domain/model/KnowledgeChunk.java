package com.gymmind.knowledge.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "knowledge_chunk")
public class KnowledgeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "chunk_key", nullable = false, length = 128)
    private String chunkKey;

    @Column(name = "generation", nullable = false)
    private long generation;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    protected KnowledgeChunk() {
    }

    private KnowledgeChunk(Long tenantId, Long documentId, String chunkKey, long generation, String text) {
        if (tenantId == null || tenantId <= 0 || documentId == null || documentId <= 0
                || chunkKey == null || chunkKey.isBlank() || text == null || text.isBlank()) {
            throw new IllegalArgumentException("invalid chunk");
        }
        this.tenantId = tenantId;
        this.documentId = documentId;
        this.chunkKey = chunkKey.trim();
        this.generation = generation;
        this.text = text;
    }

    public static KnowledgeChunk create(Long tenantId, Long documentId, String chunkKey, long generation, String text) {
        return new KnowledgeChunk(tenantId, documentId, chunkKey, generation, text);
    }

    public Long id() {
        return id;
    }

    public Long tenantId() {
        return tenantId;
    }

    public Long documentId() {
        return documentId;
    }

    public String chunkKey() {
        return chunkKey;
    }

    public long generation() {
        return generation;
    }

    public String text() {
        return text;
    }
}
