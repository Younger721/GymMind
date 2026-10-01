package com.gymmind.knowledge.application;

import com.gymmind.knowledge.domain.model.DocumentStatus;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class SnapshotKnowledgeDocumentRepository implements KnowledgeDocumentRepository {
    private final Map<Long, KnowledgeDocument> documents = new HashMap<>();
    private long nextId = 1;

    @Override
    public KnowledgeDocument save(KnowledgeDocument document) {
        if (document.id() == null) {
            document.assignId(nextId++);
        }
        documents.put(document.id(), copy(document));
        return document;
    }

    @Override
    public Optional<KnowledgeDocument> findByTenantIdAndId(Long tenantId, Long id) {
        return find(tenantId, id);
    }

    public Optional<KnowledgeDocument> findByTenantIdAndIdForUpdate(Long tenantId, Long id) {
        return find(tenantId, id);
    }

    @Override
    public List<KnowledgeDocument> findByTenantId(Long tenantId) {
        return documents.values().stream()
                .filter(document -> tenantId.equals(document.tenantId()))
                .map(SnapshotKnowledgeDocumentRepository::copy)
                .toList();
    }

    @Override
    public List<Long> findTenantIds() {
        return documents.values().stream().map(KnowledgeDocument::tenantId).filter(java.util.Objects::nonNull).distinct().toList();
    }

    private Optional<KnowledgeDocument> find(Long tenantId, Long id) {
        return Optional.ofNullable(documents.get(id))
                .filter(document -> tenantId.equals(document.tenantId()))
                .map(SnapshotKnowledgeDocumentRepository::copy);
    }

    private static KnowledgeDocument copy(KnowledgeDocument source) {
        int knowledgeMarker = source.objectKey().indexOf("/knowledge/");
        String key = source.objectKey().substring(knowledgeMarker + "/knowledge/".length());
        String keySegment = key.substring(0, key.lastIndexOf('/'));
        KnowledgeDocument copy = KnowledgeDocument.create(source.tenantId(), source.ownerUserId(),
                source.fileName(), source.contentType(), source.visibility(), keySegment);
        copy.assignId(source.id());
        if (!source.audienceRoles().isEmpty()) copy.updateAudienceRoles(source.audienceRoles());
        switch (source.status()) {
            case PARSING -> copy.markParsing();
            case READY -> {
                copy.markParsing();
                copy.markReady();
            }
            case FAILED -> {
                copy.markParsing();
                copy.markFailed();
            }
            case DELETED -> copy.tombstone();
            case UPLOADING -> {
            }
        }
        return copy;
    }
}
