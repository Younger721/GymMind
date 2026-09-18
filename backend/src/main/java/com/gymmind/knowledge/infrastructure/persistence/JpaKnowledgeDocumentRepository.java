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

    public List<KnowledgeDocument> findByTenantId(Long tenantId) {
        return delegate.findTop50ByTenantIdOrderByIdDesc(tenantId);
    }

    public List<KnowledgeDocument> findAllAccessible() {
        return delegate.findTop200ByStatusNotOrderByIdDesc(
                com.gymmind.knowledge.domain.model.DocumentStatus.DELETED);
    }
}
