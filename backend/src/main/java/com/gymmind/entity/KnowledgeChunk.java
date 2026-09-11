package com.gymmind.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "knowledge_chunks")
@EntityListeners(AuditingEntityListener.class)
public class KnowledgeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long documentId;

    @Column(nullable = false)
    private Integer chunkIndex;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String chunkText;

    @Column(length = 100)
    private String category;

    @Column(length = 50)
    private String sourceType;

    @Column(length = 500)
    private String sourceUrl;

    @Column(length = 255)
    private String documentName;

    @Column(length = 100)
    private String milvusId; // Milvus vector ID

    @Column(length = 100)
    private String esId; // Elasticsearch document ID

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
