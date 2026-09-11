package com.gymmind.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceReference {

    private Long documentId;
    private String documentName;
    private Integer chunkIndex;
    private String excerpt;
    private String category;
    private String sourceType;
    private String sourceUrl;
}
