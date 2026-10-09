package com.gymmind.knowledge.infrastructure.persistence;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;
interface SpringDataKnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
    Optional<KnowledgeDocument> findByTenantIdAndId(Long tenantId, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select document from KnowledgeDocument document where document.id = :id")
    Optional<KnowledgeDocument> findForUpdateById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select document from KnowledgeDocument document where document.tenantId = :tenantId and document.id = :id")
    Optional<KnowledgeDocument> findForUpdateByTenantIdAndId(
            @Param("tenantId") Long tenantId,
            @Param("id") Long id);

    List<KnowledgeDocument> findTop50ByTenantIdOrderByIdDesc(Long tenantId);
    List<KnowledgeDocument> findByTenantIdAndIdIn(Long tenantId, java.util.Collection<Long> ids);
    List<KnowledgeDocument> findByTenantIdAndKnowledgeKey(Long tenantId, String knowledgeKey);
    @Query("select d from KnowledgeDocument d where ((:tenantId is null and d.tenantId is null) or d.tenantId = :tenantId) and d.knowledgeType in (com.gymmind.knowledge.domain.model.KnowledgeType.BASE_PRICE, com.gymmind.knowledge.domain.model.KnowledgeType.PROMOTION_RULE)")
    List<KnowledgeDocument> findPriceRules(@Param("tenantId") Long tenantId);
    List<KnowledgeDocument> findByTenantIdIsNullOrderByIdDesc();

    List<KnowledgeDocument> findAllByOrderByIdDesc();

    @Query("select distinct document.tenantId from KnowledgeDocument document where document.tenantId is not null")
    List<Long> findDistinctTenantIds();
}
