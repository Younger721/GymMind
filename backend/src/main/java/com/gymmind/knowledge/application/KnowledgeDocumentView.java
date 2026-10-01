package com.gymmind.knowledge.application;
import com.gymmind.knowledge.domain.model.*;
import com.gymmind.iam.domain.model.RoleCode;
import java.util.List;
public record KnowledgeDocumentView(Long id, Long tenantId, Long ownerUserId, String fileName, String objectKey, DocumentVisibility visibility, DocumentStatus status, String category, boolean published, String chunkStrategy, int chunkStrategyVersion, List<RoleCode> audienceRoles) {
    public KnowledgeDocumentView(long id, Long tenantId, Long ownerUserId, String fileName, String objectKey,
                                 DocumentVisibility visibility, DocumentStatus status) {
        this(id, tenantId, ownerUserId, fileName, objectKey, visibility, status, null, false, "GENERAL", 1, List.of());
    }

    public KnowledgeDocumentView(long id, Long tenantId, Long ownerUserId, String fileName, String objectKey,
                                 DocumentVisibility visibility, DocumentStatus status, String category, boolean published) {
        this(id, tenantId, ownerUserId, fileName, objectKey, visibility, status, category, published, "GENERAL", 1, List.of());
    }

    public KnowledgeDocumentView(long id, Long tenantId, Long ownerUserId, String fileName, String objectKey,
                                 DocumentVisibility visibility, DocumentStatus status, String category, boolean published,
                                 String chunkStrategy, int chunkStrategyVersion) {
        this(id, tenantId, ownerUserId, fileName, objectKey, visibility, status, category, published, chunkStrategy, chunkStrategyVersion, List.of());
    }

    static KnowledgeDocumentView from(KnowledgeDocument d){return new KnowledgeDocumentView(d.id(),d.tenantId(),d.ownerUserId(),d.fileName(),d.objectKey(),d.visibility(),d.status(),d.category(),d.published(),d.chunkStrategy(),d.chunkStrategyVersion(),d.effectiveAudienceRoles().stream().sorted(java.util.Comparator.comparing(Enum::name)).toList());}
}
