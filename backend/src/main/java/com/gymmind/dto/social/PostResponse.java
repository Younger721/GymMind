package com.gymmind.dto.social;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PostResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String content;
    private List<String> imageUrls;
    private String postType;
    private String metadata;
    private Integer likes;
    private Integer comments;
    private Boolean isLiked;
    private String createdAt;
}
