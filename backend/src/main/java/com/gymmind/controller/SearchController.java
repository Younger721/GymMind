package com.gymmind.controller;

import com.gymmind.common.response.ApiResponse;
import com.gymmind.dto.knowledge.DocumentUploadResponse;
import com.gymmind.dto.search.ImportRequest;
import com.gymmind.dto.search.SearchResponse;
import com.gymmind.service.WebContentImportService;
import com.gymmind.service.WebSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final WebSearchService webSearchService;
    private final WebContentImportService webContentImportService;

    @GetMapping("/web")
    public ApiResponse<SearchResponse> searchWeb(@RequestParam("q") String query) {
        SearchResponse response = webSearchService.search(query);
        return ApiResponse.success(response);
    }

    @PostMapping("/import")
    public ApiResponse<DocumentUploadResponse> importFromWeb(@Valid @RequestBody ImportRequest request) {
        DocumentUploadResponse response = webContentImportService.importFromWeb(request);
        return ApiResponse.success("Content imported successfully", response);
    }
}
