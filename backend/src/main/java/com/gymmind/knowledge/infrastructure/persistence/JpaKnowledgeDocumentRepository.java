package com.gymmind.knowledge.infrastructure.persistence;
import com.gymmind.knowledge.application.KnowledgeDocumentRepository;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;
@Repository public class JpaKnowledgeDocumentRepository implements KnowledgeDocumentRepository { private final SpringDataKnowledgeDocumentRepository delegate; public JpaKnowledgeDocumentRepository(SpringDataKnowledgeDocumentRepository delegate){this.delegate=delegate;} public KnowledgeDocument save(KnowledgeDocument d){return delegate.save(d);} public Optional<KnowledgeDocument> findByTenantIdAndId(Long t,Long id){return delegate.findByTenantIdAndId(t,id);} public List<KnowledgeDocument> findByTenantId(Long t){return delegate.findTop50ByTenantIdOrderByIdDesc(t);} }
