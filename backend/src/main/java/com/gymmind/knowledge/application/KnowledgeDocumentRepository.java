package com.gymmind.knowledge.application;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import java.util.Optional;
import java.util.List;
public interface KnowledgeDocumentRepository {
    KnowledgeDocument save(KnowledgeDocument document);

    Optional<KnowledgeDocument> findByTenantIdAndId(Long tenantId, Long id);

    default Optional<KnowledgeDocument> findById(Long id) {
        return Optional.empty();
    }

    default Optional<KnowledgeDocument> findForUpdateById(Long id) {
        return findById(id);
    }

    default Optional<KnowledgeDocument> findForUpdateByTenantIdAndId(Long tenantId, Long id) {
        return findByTenantIdAndId(tenantId, id);
    }
    default List<KnowledgeDocument> findByKnowledgeKey(Long tenantId, String key) { return List.of(); }
    default List<KnowledgeDocument> findPriceRules(Long tenantId) { return List.of(); }

    default List<KnowledgeDocument> findByTenantId(Long tenantId) {
        return List.of();
    }
    default List<KnowledgeDocument> findByTenantIdAndIdIn(Long tenantId, java.util.Collection<Long> ids) {
        return List.of();
    }
    default List<KnowledgeDocument> findPlatformDocuments() { return List.of(); }

    /** Compatibility alias for callers that need the platform-wide document list. */
    default List<KnowledgeDocument> findAllAccessible() { return findAllDocuments(); }

    default List<KnowledgeDocument> findAllDocuments() { return List.of(); }
    default List<Long> findTenantIds() { return List.of(); }

}
