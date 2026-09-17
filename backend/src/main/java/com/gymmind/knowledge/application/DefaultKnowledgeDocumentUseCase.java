package com.gymmind.knowledge.application;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import com.gymmind.knowledge.infrastructure.search.KnowledgeIndexingService;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.application.TenantQuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class DefaultKnowledgeDocumentUseCase implements KnowledgeDocumentUseCase {

    private final KnowledgeDocumentRepository repository;
    private final KnowledgeObjectStore store;
    private final KnowledgeIndexingService indexing;
    private final KnowledgeIngestWorker ingestWorker;
    private final TenantQuotaService quotas;
    private final AtomicLong sequence = new AtomicLong();

    public DefaultKnowledgeDocumentUseCase(KnowledgeDocumentRepository repository, KnowledgeObjectStore store) {
        this(repository, store, null, null, null);
    }

    @Autowired
    public DefaultKnowledgeDocumentUseCase(
            KnowledgeDocumentRepository repository,
            KnowledgeObjectStore store,
            KnowledgeIndexingService indexing,
            KnowledgeIngestWorker ingestWorker,
            TenantQuotaService quotas) {
        this.repository = repository;
        this.store = store;
        this.indexing = indexing;
        this.ingestWorker = ingestWorker;
        this.quotas = quotas;
    }

    @Override
    public KnowledgeDocumentView upload(CurrentActor actor, UploadKnowledgeDocumentCommand command) {
        write(actor);
        if (quotas != null) {
            quotas.ensureKnowledgeUploadAllowed(actor.tenantId());
        }
        KnowledgeDocumentValidator.validate(command);
        Long owner = command.visibility() == DocumentVisibility.PRIVATE_USER ? actor.userId() : null;
        KnowledgeDocument document = KnowledgeDocument.create(actor.tenantId(), owner, command.fileName(),
                command.contentType(), command.visibility(), sequence.incrementAndGet());
        store.put(document.objectKey(), command.content(), command.contentType());
        KnowledgeDocument saved = repository.save(document);
        if (ingestWorker != null) {
            ingestWorker.enqueue(saved.tenantId(), saved.id());
        }
        return KnowledgeDocumentView.from(saved);
    }

    @Override
    public KnowledgeDocumentView reindex(CurrentActor actor, Long id) {
        write(actor);
        KnowledgeDocument document = get(actor, id);
        document.markParsing();
        byte[] bytes = store.read(document.objectKey());
        if (indexing != null && bytes != null) {
            if (!indexing.publish(document.tenantId(), document.id(), document.ownerUserId(),
                    document.fileName(), document.contentType(), bytes)) {
                throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE);
            }
            document.markReady();
        }
        return KnowledgeDocumentView.from(repository.save(document));
    }

    @Override
    public KnowledgeDocumentView delete(CurrentActor actor, Long id) {
        write(actor);
        KnowledgeDocument document = get(actor, id);
        document.tombstone();
        store.delete(document.objectKey());
        return KnowledgeDocumentView.from(repository.save(document));
    }

    @Override
    public List<KnowledgeDocumentView> list(CurrentActor actor) {
        if (actor == null || actor.tenantId() == null || !actor.hasPermission("knowledge:read")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return repository.findByTenantId(actor.tenantId()).stream()
                .filter(document -> document.visibility() == DocumentVisibility.TENANT
                        || (document.ownerUserId() != null && document.ownerUserId().equals(actor.userId())))
                .map(KnowledgeDocumentView::from)
                .toList();
    }

    @Override
    public boolean canRead(CurrentActor actor, Long id) {
        if (actor == null || actor.tenantId() == null) {
            return false;
        }
        KnowledgeDocument document;
        try {
            document = get(actor, id);
        } catch (BusinessException ex) {
            return false;
        }
        return document.visibility() == DocumentVisibility.TENANT || actor.userId().equals(document.ownerUserId());
    }

    private void write(CurrentActor actor) {
        if (actor == null || actor.tenantId() == null || !actor.hasPermission("knowledge:write")
                || (!actor.roles().contains(RoleCode.GYM_ADMIN) && !actor.roles().contains(RoleCode.COACH))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private KnowledgeDocument get(CurrentActor actor, Long id) {
        if (actor == null || actor.tenantId() == null || id == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return repository.findByTenantIdAndId(actor.tenantId(), id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }
}
