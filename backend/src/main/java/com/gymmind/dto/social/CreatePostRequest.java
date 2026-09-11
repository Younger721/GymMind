package com.gymmind.dto.social;

import lombok.Data;

import java.util.List;

@Data
public class CreatePostRequest {
    private String content;
    private List<String> imageUrls;
    private String postType; // WORKOUT, PROGRESS, MEAL, GENERAL
    private String metadata; // JSON string for extra data
}
