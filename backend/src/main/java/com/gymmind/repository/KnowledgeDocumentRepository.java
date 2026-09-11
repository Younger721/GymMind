package com.gymmind.repository;

import com.gymmind.entity.KnowledgeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    Page<KnowledgeDocument> findByUserId(Long userId, Pageable pageable);

    Page<KnowledgeDocument> findByUserIdAndStatus(Long userId, KnowledgeDocument.ProcessingStatus status, Pageable pageable);

    Page<KnowledgeDocument> findByUserIdAndCategory(Long userId, String category, Pageable pageable);

    Optional<KnowledgeDocument> findByIdAndUserId(Long id, Long userId);

    List<KnowledgeDocument> findByUserIdAndStatus(Long userId, KnowledgeDocument.ProcessingStatus status);

    long countByUserIdAndStatus(Long userId, KnowledgeDocument.ProcessingStatus status);
}
