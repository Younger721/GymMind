package com.gymmind.knowledge.infrastructure.persistence;

import com.gymmind.knowledge.domain.model.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataKnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {
    List<KnowledgeChunk> findAllByTenantIdAndDocumentId(Long tenantId, Long documentId);

    void deleteAllByTenantIdAndDocumentId(Long tenantId, Long documentId);
}
