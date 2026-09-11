package com.gymmind.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    private String title;
    private String url;
    private String snippet;
    private String source;
    private String publishedDate;
    private Double relevanceScore;
}
