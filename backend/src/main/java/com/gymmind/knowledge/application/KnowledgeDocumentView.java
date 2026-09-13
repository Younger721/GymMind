package com.gymmind.knowledge.application;
import com.gymmind.knowledge.domain.model.*;
public record KnowledgeDocumentView(Long id, Long tenantId, Long ownerUserId, String fileName, String objectKey, DocumentVisibility visibility, DocumentStatus status) { static KnowledgeDocumentView from(KnowledgeDocument d){return new KnowledgeDocumentView(d.id(),d.tenantId(),d.ownerUserId(),d.fileName(),d.objectKey(),d.visibility(),d.status());} }
