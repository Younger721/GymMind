package com.gymmind.controller;

import com.gymmind.common.response.ApiResponse;
import com.gymmind.dto.social.CreatePostRequest;
import com.gymmind.dto.social.PostResponse;
import com.gymmind.entity.Comment;
import com.gymmind.service.SocialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
public class SocialController {

    private final SocialService socialService;

    @PostMapping("/posts")
    public ApiResponse<PostResponse> createPost(@RequestBody CreatePostRequest request) {
        PostResponse post = socialService.createPost(request);
        return ApiResponse.success(post);
    }

    @GetMapping("/posts/feed")
    public ApiResponse<Page<PostResponse>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PostResponse> posts = socialService.getFeed(page, size);
        return ApiResponse.success(posts);
    }

    @GetMapping("/posts/trending")
    public ApiResponse<Page<PostResponse>> getTrendingPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PostResponse> posts = socialService.getTrendingPosts(page, size);
        return ApiResponse.success(posts);
    }

    @GetMapping("/posts/user/{userId}")
    public ApiResponse<Page<PostResponse>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PostResponse> posts = socialService.getUserPosts(userId, page, size);
        return ApiResponse.success(posts);
    }

    @PostMapping("/posts/{postId}/like")
    public ApiResponse<Void> likePost(@PathVariable Long postId) {
        socialService.likePost(postId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/posts/{postId}/like")
    public ApiResponse<Void> unlikePost(@PathVariable Long postId) {
        socialService.unlikePost(postId);
        return ApiResponse.success(null);
    }

    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<Comment> addComment(
            @PathVariable Long postId,
            @RequestBody String content) {
        Comment comment = socialService.addComment(postId, content);
        return ApiResponse.success(comment);
    }

    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<List<Comment>> getPostComments(@PathVariable Long postId) {
        List<Comment> comments = socialService.getPostComments(postId);
        return ApiResponse.success(comments);
    }

    @DeleteMapping("/posts/{postId}")
    public ApiResponse<Void> deletePost(@PathVariable Long postId) {
        socialService.deletePost(postId);
        return ApiResponse.success(null);
    }
}
