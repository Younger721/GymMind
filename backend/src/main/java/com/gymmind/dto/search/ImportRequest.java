package com.gymmind.dto.search;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ImportRequest {

    @NotBlank(message = "URL is required")
    private String url;

    @NotBlank(message = "Title is required")
    private String title;

    private String category;
}
