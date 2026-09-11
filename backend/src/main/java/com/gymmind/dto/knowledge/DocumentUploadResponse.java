package com.gymmind.dto.knowledge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResponse {

    private Long documentId;
    private String filename;
    private String fileType;
    private Long fileSize;
    private String status;
    private String message;
}
