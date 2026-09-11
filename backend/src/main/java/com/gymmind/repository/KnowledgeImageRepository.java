package com.gymmind.repository;

import com.gymmind.entity.KnowledgeImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeImageRepository extends JpaRepository<KnowledgeImage, Long> {

    List<KnowledgeImage> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<KnowledgeImage> findByIdAndUserId(Long id, Long userId);

    List<KnowledgeImage> findByUserIdAndCategory(Long userId, String category);

    List<KnowledgeImage> findByUserIdAndStatus(Long userId, String status);

    long countByUserIdAndStatus(Long userId, String status);
}
