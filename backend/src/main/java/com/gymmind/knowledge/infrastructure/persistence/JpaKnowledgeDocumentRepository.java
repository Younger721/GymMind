package com.gymmind.knowledge.infrastructure.persistence;
import com.gymmind.knowledge.application.KnowledgeDocumentRepository;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;
@Repository
public class JpaKnowledgeDocumentRepository implements KnowledgeDocumentRepository {
    private final SpringDataKnowledgeDocumentRepository delegate;

    public JpaKnowledgeDocumentRepository(SpringDataKnowledgeDocumentRepository delegate) {
        this.delegate = delegate;
    }

    public KnowledgeDocument save(KnowledgeDocument document) {
        return delegate.save(document);
    }

    public Optional<KnowledgeDocument> findByTenantIdAndId(Long tenantId, Long id) {
        return delegate.findByTenantIdAndId(tenantId, id);
    }

    public Optional<KnowledgeDocument> findById(Long id) {
        return delegate.findById(id);
    }

    public Optional<KnowledgeDocument> findForUpdateById(Long id) {
        return delegate.findForUpdateById(id);
    }

    public Optional<KnowledgeDocument> findForUpdateByTenantIdAndId(Long tenantId, Long id) {
        return delegate.findForUpdateByTenantIdAndId(tenantId, id);
    }

    public List<KnowledgeDocument> findByTenantId(Long tenantId) {
        return delegate.findTop50ByTenantIdOrderByIdDesc(tenantId);
    }
    public List<KnowledgeDocument> findByTenantIdAndIdIn(Long tenantId, java.util.Collection<Long> ids) {
        return ids == null || ids.isEmpty() ? List.of() : delegate.findByTenantIdAndIdIn(tenantId, ids);
    }
    public List<KnowledgeDocument> findPlatformDocuments() { return delegate.findByTenantIdIsNullOrderByIdDesc(); }

    public List<KnowledgeDocument> findAllDocuments() {
        return delegate.findAllByOrderByIdDesc();
    }

    public List<Long> findTenantIds() { return delegate.findDistinctTenantIds(); }

}
