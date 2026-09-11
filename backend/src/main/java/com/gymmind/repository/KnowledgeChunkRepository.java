package com.gymmind.repository;

import com.gymmind.entity.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {

    List<KnowledgeChunk> findByDocumentId(Long documentId);

    List<KnowledgeChunk> findByUserIdAndDocumentId(Long userId, Long documentId);

    void deleteByDocumentId(Long documentId);

    long countByDocumentId(Long documentId);
}
