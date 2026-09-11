package com.gymmind.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "knowledge_documents")
@EntityListeners(AuditingEntityListener.class)
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(nullable = false, length = 500)
    private String originalFilename;

    @Column(nullable = false, length = 50)
    private String fileType; // PDF, TXT, MARKDOWN, WORD

    @Column(nullable = false)
    private Long fileSize;

    @Column(length = 500)
    private String storageUrl; // MinIO object key

    @Column(length = 100)
    private String category; // FITNESS, NUTRITION, RESEARCH, OTHER

    @Column(length = 50)
    private String sourceType; // UPLOAD, WEB_SEARCH, PAPER

    @Column(length = 500)
    private String sourceUrl;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ProcessingStatus status;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    @Column
    private Integer chunkCount;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime processedAt;

    public enum ProcessingStatus {
        PENDING, PROCESSING, SUCCESS, FAILED
    }
}
