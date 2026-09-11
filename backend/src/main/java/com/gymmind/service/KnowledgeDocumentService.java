package com.gymmind.service;

import com.gymmind.common.exception.BusinessException;
import com.gymmind.dto.knowledge.DocumentUploadResponse;
import com.gymmind.dto.knowledge.KnowledgeDocumentResponse;
import com.gymmind.entity.KnowledgeDocument;
import com.gymmind.repository.KnowledgeDocumentRepository;
import com.gymmind.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentService {

    private final KnowledgeDocumentRepository documentRepository;
    private final MinioService minioService;
    private final DocumentProcessingService documentProcessingService;
    private final KnowledgeChunkRepository chunkRepository;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "txt");
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    @Transactional
    public DocumentUploadResponse uploadDocument(MultipartFile file, String category) {
        Long userId = SecurityUtils.getCurrentUserId();

        // Validate file
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);

        try {
            // Upload to MinIO
            String storageUrl = minioService.uploadFile(userId, file);

            // Create document record
            KnowledgeDocument document = KnowledgeDocument.builder()
                    .userId(userId)
                    .filename(storageUrl)
                    .originalFilename(originalFilename)
                    .fileType(fileExtension.toUpperCase())
                    .fileSize(file.getSize())
                    .storageUrl(storageUrl)
                    .category(category != null ? category : "OTHER")
                    .sourceType("UPLOAD")
                    .status(KnowledgeDocument.ProcessingStatus.PENDING)
                    .build();

            document = documentRepository.save(document);
            log.info("Created document record: id={}, userId={}", document.getId(), userId);

            // Trigger async processing
            documentProcessingService.processDocumentAsync(document.getId());

            return DocumentUploadResponse.builder()
                    .documentId(document.getId())
                    .filename(originalFilename)
                    .fileType(fileExtension.toUpperCase())
                    .fileSize(file.getSize())
                    .status("PENDING")
                    .message("Document uploaded successfully and queued for processing")
                    .build();

        } catch (IOException e) {
            log.error("Failed to upload document", e);
            throw new BusinessException("Failed to upload document: " + e.getMessage());
        }
    }

    public Page<KnowledgeDocumentResponse> getDocuments(Pageable pageable, String status, String category) {
        Long userId = SecurityUtils.getCurrentUserId();

        Page<KnowledgeDocument> documents;

        if (status != null && !status.isEmpty()) {
            KnowledgeDocument.ProcessingStatus processingStatus =
                    KnowledgeDocument.ProcessingStatus.valueOf(status.toUpperCase());
            documents = documentRepository.findByUserIdAndStatus(userId, processingStatus, pageable);
        } else if (category != null && !category.isEmpty()) {
            documents = documentRepository.findByUserIdAndCategory(userId, category, pageable);
        } else {
            documents = documentRepository.findByUserId(userId, pageable);
        }

        return documents.map(this::toResponse);
    }

    public KnowledgeDocumentResponse getDocument(Long documentId) {
        Long userId = SecurityUtils.getCurrentUserId();
        KnowledgeDocument document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new BusinessException(404, "Document not found"));

        return toResponse(document);
    }

    @Transactional
    public void deleteDocument(Long documentId) {
        Long userId = SecurityUtils.getCurrentUserId();
        KnowledgeDocument document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new BusinessException(404, "Document not found"));

        // Delete chunks from database
        chunkRepository.deleteByDocumentId(documentId);

        // Delete from MinIO
        if (document.getStorageUrl() != null) {
            minioService.deleteFile(document.getStorageUrl());
        }

        // TODO: Delete from Milvus and Elasticsearch

        // Delete from database
        documentRepository.delete(document);
        log.info("Deleted document: id={}, userId={}", documentId, userId);
    }

    @Transactional
    public void retryProcessing(Long documentId) {
        Long userId = SecurityUtils.getCurrentUserId();
        KnowledgeDocument document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new BusinessException(404, "Document not found"));

        if (document.getStatus() != KnowledgeDocument.ProcessingStatus.FAILED) {
            throw new BusinessException(400, "Only failed documents can be retried");
        }

        document.setStatus(KnowledgeDocument.ProcessingStatus.PENDING);
        document.setFailureReason(null);
        documentRepository.save(document);

        documentProcessingService.processDocumentAsync(documentId);
        log.info("Retry processing for document: id={}", documentId);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(400, "File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(400, "File size exceeds maximum limit of 50MB");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw new BusinessException(400, "Invalid filename");
        }

        String extension = getFileExtension(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException(400, "File type not supported. Allowed types: PDF, TXT");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    private KnowledgeDocumentResponse toResponse(KnowledgeDocument document) {
        return KnowledgeDocumentResponse.builder()
                .id(document.getId())
                .userId(document.getUserId())
                .filename(document.getFilename())
                .originalFilename(document.getOriginalFilename())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .category(document.getCategory())
                .sourceType(document.getSourceType())
                .sourceUrl(document.getSourceUrl())
                .status(document.getStatus().name())
                .failureReason(document.getFailureReason())
                .chunkCount(document.getChunkCount())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .processedAt(document.getProcessedAt())
                .build();
    }
}
