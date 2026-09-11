package com.gymmind.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalResult {

    private Long documentId;
    private Integer chunkIndex;
    private String chunkText;
    private String documentName;
    private String category;
    private String sourceType;
    private String sourceUrl;
    private Double score;
    private String source; // "vector", "keyword", "hybrid"
}
