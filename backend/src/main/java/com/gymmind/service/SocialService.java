package com.gymmind.service;

import com.gymmind.dto.social.CreatePostRequest;
import com.gymmind.dto.social.PostResponse;
import com.gymmind.entity.Comment;
import com.gymmind.entity.Post;
import com.gymmind.entity.PostLike;
import com.gymmind.entity.User;
import com.gymmind.repository.CommentRepository;
import com.gymmind.repository.PostLikeRepository;
import com.gymmind.repository.PostRepository;
import com.gymmind.repository.UserRepository;
import com.gymmind.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;

    @Transactional
    public PostResponse createPost(CreatePostRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        Post post = Post.builder()
                .userId(userId)
                .content(request.getContent())
                .imageUrls(request.getImageUrls())
                .postType(request.getPostType() != null ? request.getPostType() : "GENERAL")
                .metadata(request.getMetadata())
                .likes(0)
                .comments(0)
                .build();

        post = postRepository.save(post);

        log.info("Post created: postId={}, userId={}", post.getId(), userId);

        return convertToResponse(post, userId);
    }

    public Page<PostResponse> getFeed(int page, int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);

        Page<Post> posts = postRepository.findAllByOrderByCreatedAtDesc(pageable);

        return posts.map(post -> convertToResponse(post, userId));
    }

    public Page<PostResponse> getTrendingPosts(int page, int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);

        // Get posts from last 7 days
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        Page<Post> posts = postRepository.findTrendingPosts(since, pageable);

        return posts.map(post -> convertToResponse(post, userId));
    }

    public Page<PostResponse> getUserPosts(Long targetUserId, int page, int size) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);

        Page<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(targetUserId, pageable);

        return posts.map(post -> convertToResponse(post, currentUserId));
    }

    @Transactional
    public void likePost(Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();

        // Check if already liked
        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            return;
        }

        PostLike like = PostLike.builder()
                .postId(postId)
                .userId(userId)
                .build();

        postLikeRepository.save(like);

        // Update post likes count
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setLikes(post.getLikes() + 1);
        postRepository.save(post);

        // Notify post owner
        if (!post.getUserId().equals(userId)) {
            webSocketService.sendNotification(
                    post.getUserId().toString(),
                    "新的点赞",
                    "有人点赞了你的动态",
                    "info"
            );
        }

        log.info("Post liked: postId={}, userId={}", postId, userId);
    }

    @Transactional
    public void unlikePost(Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();

        postLikeRepository.deleteByPostIdAndUserId(postId, userId);

        // Update post likes count
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setLikes(Math.max(0, post.getLikes() - 1));
        postRepository.save(post);

        log.info("Post unliked: postId={}, userId={}", postId, userId);
    }

    @Transactional
    public Comment addComment(Long postId, String content) {
        Long userId = SecurityUtils.getCurrentUserId();

        Comment comment = Comment.builder()
                .postId(postId)
                .userId(userId)
                .content(content)
                .build();

        comment = commentRepository.save(comment);

        // Update post comments count
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setComments(post.getComments() + 1);
        postRepository.save(post);

        // Notify post owner
        if (!post.getUserId().equals(userId)) {
            webSocketService.sendNotification(
                    post.getUserId().toString(),
                    "新的评论",
                    "有人评论了你的动态",
                    "info"
            );
        }

        log.info("Comment added: postId={}, userId={}", postId, userId);

        return comment;
    }

    public List<Comment> getPostComments(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtDesc(postId);
    }

    @Transactional
    public void deletePost(Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this post");
        }

        postRepository.delete(post);

        log.info("Post deleted: postId={}, userId={}", postId, userId);
    }

    private PostResponse convertToResponse(Post post, Long currentUserId) {
        User author = userRepository.findById(post.getUserId())
                .orElse(null);

        boolean isLiked = postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUserId);

        return PostResponse.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .username(author != null ? author.getUsername() : "Unknown")
                .content(post.getContent())
                .imageUrls(post.getImageUrls())
                .postType(post.getPostType())
                .metadata(post.getMetadata())
                .likes(post.getLikes())
                .comments(post.getComments())
                .isLiked(isLiked)
                .createdAt(post.getCreatedAt())
                .build();
    }
}
