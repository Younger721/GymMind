package com.gymmind.service;

import com.gymmind.entity.KnowledgeDocument;
import com.gymmind.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final KnowledgeDocumentRepository documentRepository;
    private final MinioService minioService;
    private final DocumentParserService documentParserService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final VectorStorageService vectorStorageService;

    @Async
    @Transactional
    public void processDocumentAsync(Long documentId) {
        log.info("Starting async processing for document: {}", documentId);

        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        try {
            // Update status to PROCESSING
            document.setStatus(KnowledgeDocument.ProcessingStatus.PROCESSING);
            documentRepository.save(document);

            // Step 1: Download file from MinIO
            InputStream fileStream = minioService.getFile(document.getStorageUrl());

            // Step 2: Parse document
            String content = documentParserService.parseDocument(fileStream, document.getFileType());
            fileStream.close();

            if (content == null || content.trim().isEmpty()) {
                throw new RuntimeException("Extracted content is empty");
            }

            log.info("Parsed document {}: {} characters", documentId, content.length());

            // Step 3: Chunk content
            List<String> chunks = chunkingService.chunkText(content);
            log.info("Created {} chunks for document {}", chunks.size(), documentId);

            // Step 4: Generate embeddings and store
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);

                // Generate embedding
                float[] embedding = embeddingService.generateEmbedding(chunk);

                // Store in Milvus and Elasticsearch
                vectorStorageService.storeChunk(
                        document.getUserId(),
                        document.getId(),
                        document.getOriginalFilename(),
                        document.getCategory(),
                        document.getSourceType(),
                        document.getSourceUrl(),
                        i,
                        chunk,
                        embedding
                );
            }

            // Update document status to SUCCESS
            document.setStatus(KnowledgeDocument.ProcessingStatus.SUCCESS);
            document.setChunkCount(chunks.size());
            document.setProcessedAt(LocalDateTime.now());
            document.setFailureReason(null);
            documentRepository.save(document);

            log.info("Successfully processed document: {}", documentId);

        } catch (Exception e) {
            log.error("Failed to process document: {}", documentId, e);

            // Update document status to FAILED
            document.setStatus(KnowledgeDocument.ProcessingStatus.FAILED);
            document.setFailureReason(e.getMessage());
            documentRepository.save(document);
        }
    }
}
