package com.gymmind.controller;

import com.gymmind.common.response.ApiResponse;
import com.gymmind.dto.knowledge.DocumentUploadResponse;
import com.gymmind.dto.knowledge.KnowledgeDocumentResponse;
import com.gymmind.service.KnowledgeDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeDocumentService knowledgeDocumentService;

    @PostMapping("/upload")
    public ApiResponse<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category) {

        DocumentUploadResponse response = knowledgeDocumentService.uploadDocument(file, category);
        return ApiResponse.success("Document uploaded successfully", response);
    }

    @GetMapping
    public ApiResponse<Page<KnowledgeDocumentResponse>> getDocuments(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "category", required = false) String category) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<KnowledgeDocumentResponse> documents =
                knowledgeDocumentService.getDocuments(pageable, status, category);

        return ApiResponse.success(documents);
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeDocumentResponse> getDocument(@PathVariable Long id) {
        KnowledgeDocumentResponse document = knowledgeDocumentService.getDocument(id);
        return ApiResponse.success(document);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDocument(@PathVariable Long id) {
        knowledgeDocumentService.deleteDocument(id);
        return ApiResponse.success("Document deleted successfully", null);
    }

    @PostMapping("/{id}/retry")
    public ApiResponse<Void> retryProcessing(@PathVariable Long id) {
        knowledgeDocumentService.retryProcessing(id);
        return ApiResponse.success("Document queued for reprocessing", null);
    }
}
