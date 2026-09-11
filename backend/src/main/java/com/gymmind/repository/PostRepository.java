package com.gymmind.repository;

import com.gymmind.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY (p.likes * 0.7 + p.comments * 0.3) DESC, p.createdAt DESC")
    Page<Post> findTrendingPosts(LocalDateTime since, Pageable pageable);
}
