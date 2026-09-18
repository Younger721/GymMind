package com.gymmind.knowledge.application;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import java.util.Optional;
import java.util.List;
public interface KnowledgeDocumentRepository {
    KnowledgeDocument save(KnowledgeDocument document);

    Optional<KnowledgeDocument> findByTenantIdAndId(Long tenantId, Long id);

    Optional<KnowledgeDocument> findById(Long id);

    default List<KnowledgeDocument> findByTenantId(Long tenantId) {
        return List.of();
    }

    /** 平台管理员白名单：跨租户读取全部未删除文档 */
    default List<KnowledgeDocument> findAllAccessible() {
        return List.of();
    }
}
