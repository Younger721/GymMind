package com.gymmind.dto.knowledge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocumentResponse {

    private Long id;
    private Long userId;
    private String filename;
    private String originalFilename;
    private String fileType;
    private Long fileSize;
    private String category;
    private String sourceType;
    private String sourceUrl;
    private String status;
    private String failureReason;
    private Integer chunkCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime processedAt;
}
