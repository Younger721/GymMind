package com.gymmind.service;

import com.gymmind.common.exception.BusinessException;
import com.gymmind.dto.knowledge.DocumentUploadResponse;
import com.gymmind.dto.search.ImportRequest;
import com.gymmind.entity.KnowledgeDocument;
import com.gymmind.repository.KnowledgeDocumentRepository;
import com.gymmind.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebContentImportService {

    private final KnowledgeDocumentRepository documentRepository;
    private final MinioService minioService;
    private final DocumentProcessingService documentProcessingService;

    @Transactional
    public DocumentUploadResponse importFromWeb(ImportRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        log.info("Importing web content from: {}", request.getUrl());

        try {
            // Fetch web content
            String content = fetchWebContent(request.getUrl());

            if (content == null || content.trim().isEmpty()) {
                throw new BusinessException(400, "Failed to extract content from URL");
            }

            // Save content as temporary file
            File tempFile = createTempFile(content);

            // Upload to MinIO
            String filename = UUID.randomUUID().toString() + ".txt";
            String storageUrl = minioService.uploadFile(userId, tempFile, filename);

            // Clean up temp file
            tempFile.delete();

            // Create document record
            KnowledgeDocument document = KnowledgeDocument.builder()
                    .userId(userId)
                    .filename(filename)
                    .originalFilename(request.getTitle() + ".txt")
                    .fileType("TXT")
                    .fileSize((long) content.length())
                    .storageUrl(storageUrl)
                    .category(request.getCategory() != null ? request.getCategory() : "OTHER")
                    .sourceType("WEB_SEARCH")
                    .sourceUrl(request.getUrl())
                    .status(KnowledgeDocument.ProcessingStatus.PENDING)
                    .build();

            document = documentRepository.save(document);
            log.info("Created document from web: id={}, url={}", document.getId(), request.getUrl());

            // Trigger async processing
            documentProcessingService.processDocumentAsync(document.getId());

            return DocumentUploadResponse.builder()
                    .documentId(document.getId())
                    .filename(request.getTitle())
                    .fileType("TXT")
                    .fileSize((long) content.length())
                    .status("PENDING")
                    .message("Web content imported successfully and queued for processing")
                    .build();

        } catch (Exception e) {
            log.error("Failed to import web content", e);
            throw new BusinessException("Failed to import web content: " + e.getMessage());
        }
    }

    private String fetchWebContent(String url) {
        try {
            // Validate URL
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                throw new IllegalArgumentException("Invalid URL format");
            }

            // Fetch and parse HTML
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            // Extract main content
            String title = doc.title();
            String body = doc.body().text();

            // Combine title and body
            StringBuilder content = new StringBuilder();
            content.append("# ").append(title).append("\n\n");
            content.append("Source: ").append(url).append("\n\n");
            content.append(body);

            return content.toString();

        } catch (Exception e) {
            log.error("Failed to fetch web content from: {}", url, e);
            return null;
        }
    }

    private File createTempFile(String content) throws Exception {
        File tempFile = File.createTempFile("web_import_", ".txt");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
        }
        return tempFile;
    }
}
