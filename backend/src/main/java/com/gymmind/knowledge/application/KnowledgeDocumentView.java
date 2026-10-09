package com.gymmind.knowledge.application;
import com.gymmind.knowledge.domain.model.*;
import com.gymmind.iam.domain.model.RoleCode;
import java.util.List;
public record KnowledgeDocumentView(Long id, Long tenantId, Long ownerUserId, String uploaderName, String fileName, String objectKey, DocumentVisibility visibility, DocumentStatus status, String category, boolean published, String chunkStrategy, int chunkStrategyVersion, List<RoleCode> audienceRoles, String knowledgeKey, int knowledgeVersion, com.gymmind.knowledge.domain.model.KnowledgeType knowledgeType, String applicableProduct, java.math.BigDecimal priceAmount, String discountType, java.math.BigDecimal discountValue, java.time.LocalDateTime effectiveFrom, java.time.LocalDateTime effectiveTo, String promotionConditions, int promotionPriority) {
    public KnowledgeDocumentView(long id, Long tenantId, Long ownerUserId, String fileName, String objectKey,
                                 DocumentVisibility visibility, DocumentStatus status) {
        this(id, tenantId, ownerUserId, null, fileName, objectKey, visibility, status, null, false, "GENERAL", 1, List.of(), null, 1, null, null, null, null, null, null, null, null, 0);
    }

    public KnowledgeDocumentView(long id, Long tenantId, Long ownerUserId, String fileName, String objectKey,
                                 DocumentVisibility visibility, DocumentStatus status, String category, boolean published) {
        this(id, tenantId, ownerUserId, null, fileName, objectKey, visibility, status, category, published, "GENERAL", 1, List.of(), null, 1, null, null, null, null, null, null, null, null, 0);
    }

    public KnowledgeDocumentView(long id, Long tenantId, Long ownerUserId, String fileName, String objectKey,
                                 DocumentVisibility visibility, DocumentStatus status, String category, boolean published,
                                 String chunkStrategy, int chunkStrategyVersion) {
        this(id, tenantId, ownerUserId, null, fileName, objectKey, visibility, status, category, published, chunkStrategy, chunkStrategyVersion, List.of(), null, 1, null, null, null, null, null, null, null, null, 0);
    }

    static KnowledgeDocumentView from(KnowledgeDocument d){return from(d, null);}
    static KnowledgeDocumentView from(KnowledgeDocument d, String uploaderName){return new KnowledgeDocumentView(d.id(),d.tenantId(),d.ownerUserId(),uploaderName,d.fileName(),d.objectKey(),d.visibility(),d.status(),d.category(),effectivePublished(d),d.chunkStrategy(),d.chunkStrategyVersion(),d.effectiveAudienceRoles().stream().sorted(java.util.Comparator.comparing(Enum::name)).toList(),d.knowledgeKey(),d.knowledgeVersion(),d.knowledgeType(),d.applicableProduct(),d.priceAmount(),d.discountType(),d.discountValue(),d.effectiveFrom(),d.effectiveTo(),d.promotionConditions(),d.promotionPriority());}

    private static boolean effectivePublished(KnowledgeDocument document) {
        return document.status() == DocumentStatus.READY && document.published();
    }
}
