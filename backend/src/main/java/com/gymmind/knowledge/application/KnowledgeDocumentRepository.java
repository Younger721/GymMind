package com.gymmind.knowledge.application;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import java.util.Optional;
public interface KnowledgeDocumentRepository { KnowledgeDocument save(KnowledgeDocument document); Optional<KnowledgeDocument> findByTenantIdAndId(Long tenantId, Long id); }
