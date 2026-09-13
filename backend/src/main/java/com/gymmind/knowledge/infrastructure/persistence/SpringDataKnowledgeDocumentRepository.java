package com.gymmind.knowledge.infrastructure.persistence;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
interface SpringDataKnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> { Optional<KnowledgeDocument> findByTenantIdAndId(Long tenantId, Long id); }
