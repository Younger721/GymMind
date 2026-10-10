package com.gymmind.knowledge.application;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.knowledge.domain.model.DocumentStatus;
import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import com.gymmind.knowledge.infrastructure.search.KnowledgeIndexingService;
import com.gymmind.knowledge.infrastructure.parser.ChunkStrategy;
import com.gymmind.knowledge.infrastructure.parser.ChunkStrategySelector;
import com.gymmind.knowledge.infrastructure.parser.DocumentParserRegistry;
import com.gymmind.ai.application.ChatModelGateway;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.application.TenantQuotaService;
import com.gymmind.tenancy.application.TenantQuotaServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DefaultKnowledgeDocumentUseCase implements KnowledgeDocumentUseCase {
    private static final ConcurrentHashMap<String, Object> VERSION_LOCKS = new ConcurrentHashMap<>();

    private final KnowledgeDocumentRepository repository;
    private final KnowledgeObjectStore store;
    private final KnowledgeIndexingService indexing;
    private final KnowledgeIngestWorker ingestWorker;
    private final TenantQuotaService quotas;
    private final DocumentParserRegistry parser = DocumentParserRegistry.standard();
    private final ChunkStrategySelector strategySelector;
    private final UserAccountRepository users;
    private final KnowledgeChunkArtifactService chunkArtifacts;

    public DefaultKnowledgeDocumentUseCase(KnowledgeDocumentRepository repository, KnowledgeObjectStore store) {
        this(repository, store, null, null, TenantQuotaServices.unboundedForLegacyConstruction(), null, null, null);
    }

    public DefaultKnowledgeDocumentUseCase(
            KnowledgeDocumentRepository repository,
            KnowledgeObjectStore store,
            KnowledgeIndexingService indexing,
            KnowledgeIngestWorker ingestWorker,
            TenantQuotaService quotas) {
        this(repository, store, indexing, ingestWorker, quotas, null, null, null);
    }

    @Autowired
    public DefaultKnowledgeDocumentUseCase(
            KnowledgeDocumentRepository repository,
            KnowledgeObjectStore store,
            KnowledgeIndexingService indexing,
            KnowledgeIngestWorker ingestWorker,
            TenantQuotaService quotas,
            ChatModelGateway chatModel,
            UserAccountRepository users,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            KnowledgeChunkArtifactService chunkArtifacts) {
        this.repository = repository;
        this.store = store;
        this.indexing = indexing;
        this.ingestWorker = ingestWorker;
        this.quotas = quotas == null ? TenantQuotaServices.unboundedForLegacyConstruction() : quotas;
        this.strategySelector = new ChunkStrategySelector(chatModel);
        this.users = users;
        this.chunkArtifacts = chunkArtifacts;
    }

    @Override
    public KnowledgeDocumentView upload(CurrentActor actor, UploadKnowledgeDocumentCommand command) {
        write(actor);
        Long tenantId = actor.isPlatformAdmin() ? null : ActorAccess.requireTenantId(actor);
        if (tenantId != null) quotas.ensureKnowledgeUploadAllowed(tenantId);
        KnowledgeDocumentValidator.validate(command);
        Long owner = command.visibility() == DocumentVisibility.PRIVATE_USER ? actor.userId() : null;
        KnowledgeDocument replacement = null;
        if (command.replaceDocumentId() != null) {
            replacement = tenantId == null
                    ? repository.findForUpdateById(command.replaceDocumentId()).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND))
                    : repository.findForUpdateByTenantIdAndId(tenantId, command.replaceDocumentId()).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        }
        if (replacement == null && command.knowledgeKey() != null && !command.knowledgeKey().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (replacement != null && !Objects.equals(replacement.tenantId(), tenantId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (replacement != null && replacement.status() == DocumentStatus.DELETED) throw new BusinessException(ErrorCode.CONFLICT);
        KnowledgeDocument document = KnowledgeDocument.create(tenantId, owner, command.fileName(),
                command.contentType(), command.visibility(), UUID.randomUUID().toString());
        if (command.knowledgeKey() != null && !command.knowledgeKey().isBlank()) {
            if (replacement != null) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
            document.assignVersion(command.knowledgeKey(), 1);
        }
        if (replacement != null) {
            String key = replacement.knowledgeKey() == null ? "document-" + replacement.id() : replacement.knowledgeKey();
            synchronized (VERSION_LOCKS.computeIfAbsent(versionLockKey(tenantId, key), ignored -> new Object())) {
                if (replacement.knowledgeKey() == null) {
                    replacement.assignVersion(key, replacement.knowledgeVersion());
                    repository.save(replacement);
                }
                document.assignVersion(key, repository.findByKnowledgeKey(tenantId, key).stream()
                        .mapToInt(KnowledgeDocument::knowledgeVersion)
                        .max().orElse(replacement.knowledgeVersion()) + 1);
            }
        }
        document.recordUploader(actor.userId());
        document.updateCategory(command.category());
        document.configureKnowledge(command.knowledgeType(), command.applicableProduct(), command.priceAmount(), command.discountType(),
                command.discountValue(), command.effectiveFrom(), command.effectiveTo(), command.conditions(), command.promotionPriority());
        if (replacement != null && (replacement.knowledgeType() != document.knowledgeType()
                || !Objects.equals(replacement.applicableProduct(), document.applicableProduct()))) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        document.updateAudienceRoles(resolveAudienceRoles(actor, tenantId, command.visibility(), command.audienceRoles()));
        document.updateChunkStrategy(selectChunkStrategy(command), 1);
        store.put(document.objectKey(), command.content(), command.contentType());
        KnowledgeDocument saved = repository.save(document);
        if (ingestWorker != null) {
            ingestWorker.enqueue(saved.tenantId(), saved.id());
        }
        return view(saved);
    }

    private static String versionLockKey(Long tenantId, String knowledgeKey) {
        return (tenantId == null ? "platform" : tenantId.toString()) + "|" + knowledgeKey;
    }

    @Override
    public KnowledgeDocumentView find(CurrentActor actor, Long id) {
        requireRead(actor);
        KnowledgeDocument document = get(actor, id);
        ensureReadable(actor, document);
        return view(document);
    }

    @Override
    @Transactional
    public KnowledgeDocumentView update(CurrentActor actor, Long id, UpdateKnowledgeDocumentCommand command) {
        write(actor);
        if (command == null || command.visibility() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        KnowledgeDocument document = getForUpdate(actor, id);
        Long newOwner = command.visibility() == DocumentVisibility.PRIVATE_USER ? actor.userId() : null;
        Set<RoleCode> newAudience = command.audienceRoles() == null
                ? document.effectiveAudienceRoles()
                : resolveAudienceRoles(actor, document.tenantId(), command.visibility(), command.audienceRoles());
        boolean changed = document.visibility() != command.visibility()
                || !Objects.equals(document.ownerUserId(), newOwner)
                || !document.effectiveAudienceRoles().equals(newAudience);
        if (changed && restrictsVisibility(document, command.visibility(), newOwner) && indexing != null) {
            indexing.deleteDocument(document.tenantId(), document.id());
        }
        if (changed && !document.effectiveAudienceRoles().equals(newAudience) && indexing != null) {
            indexing.deleteDocument(document.tenantId(), document.id());
        }
        if (changed) {
            document.updateVisibility(command.visibility(), actor.userId());
            document.updateAudienceRoles(newAudience);
        }
        KnowledgeDocument saved = repository.save(document);
        if (changed && ingestWorker != null) {
            ingestWorker.enqueue(saved.tenantId(), saved.id());
        }
        return view(saved);
    }

    @Override
    @Transactional
    public KnowledgeDocumentView publish(CurrentActor actor, Long id, boolean published) {
        write(actor);
        KnowledgeDocument document = getForUpdate(actor, id);
        if (published) {
            document.publish();
        } else {
            document.unpublish();
            if (indexing != null) {
                indexing.deleteDocument(document.tenantId(), document.id());
            }
        }
        return view(repository.save(document));
    }

    @Override
    @Transactional
    public KnowledgeDocumentView delete(CurrentActor actor, Long id) {
        write(actor);
        KnowledgeDocument document = getForUpdate(actor, id);
        if (indexing != null) {
            indexing.deleteDocument(document.tenantId(), document.id());
        }
        document.tombstone();
        if (chunkArtifacts != null && document.chunkArtifactKey() != null) {
            chunkArtifacts.deleteArtifact(document.chunkArtifactKey());
        }
        store.delete(document.objectKey());
        return view(repository.save(document));
    }

    @Override
    public List<KnowledgeDocumentView> list(CurrentActor actor) {
        requireRead(actor);
        if (actor.isPlatformAdmin()) {
            return repository.findAllDocuments().stream()
                    .filter(document -> document.status() != DocumentStatus.DELETED)
                    .map(this::view)
                    .toList();
        }
        Long tenantId = ActorAccess.requireTenantId(actor);
        Stream<KnowledgeDocument> stream = repository.findByTenantId(tenantId).stream()
                .filter(document -> document.status() != DocumentStatus.DELETED);
        if (!ActorAccess.canManageTenant(actor)) {
            stream = stream.filter(document -> document.visibility() == DocumentVisibility.TENANT
                    || (document.ownerUserId() != null && document.ownerUserId().equals(actor.userId())))
                    .filter(document -> document.effectiveAudienceRoles().stream().anyMatch(actor.roles()::contains));
        }
        return stream.map(this::view).toList();
    }

    private KnowledgeDocumentView view(KnowledgeDocument document) {
        if (users == null || document.uploaderUserId() == null) {
            return KnowledgeDocumentView.from(document);
        }
        String uploader = users.findById(document.uploaderUserId())
                .map(user -> user.getDisplayName())
                .orElse(null);
        return KnowledgeDocumentView.from(document, uploader);
    }

    @Override
    public boolean canRead(CurrentActor actor, Long id) {
        if (actor == null || id == null) {
            return false;
        }
        try {
            KnowledgeDocument document = get(actor, id);
            ensureReadable(actor, document);
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    private void requireRead(CurrentActor actor) {
        if (actor == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (ActorAccess.isSuperAdmin(actor)) {
            return;
        }
        if (actor.tenantId() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void write(CurrentActor actor) {
        if (actor == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (ActorAccess.isSuperAdmin(actor)) return;
        ActorAccess.requireTenantId(actor);
        if (!actor.roles().contains(RoleCode.GYM_ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private KnowledgeDocument get(CurrentActor actor, Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (actor.isPlatformAdmin()) {
            return repository.findById(id)
                    .filter(document -> document.status() != DocumentStatus.DELETED)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        }
        Long tenantId = ActorAccess.requireTenantId(actor);
        return repository.findByTenantIdAndId(tenantId, id)
                .filter(document -> document.status() != DocumentStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private KnowledgeDocument getForUpdate(CurrentActor actor, Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (actor.isPlatformAdmin()) {
            return repository.findForUpdateById(id)
                    .filter(document -> document.status() != DocumentStatus.DELETED)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        }
        Long tenantId = ActorAccess.requireTenantId(actor);
        return repository.findForUpdateByTenantIdAndId(tenantId, id)
                .filter(document -> document.status() != DocumentStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private static boolean restrictsVisibility(KnowledgeDocument document,
                                               DocumentVisibility newVisibility,
                                               Long newOwner) {
        return document.visibility() == DocumentVisibility.TENANT
                && newVisibility == DocumentVisibility.PRIVATE_USER
                || document.visibility() == DocumentVisibility.PRIVATE_USER
                && newVisibility == DocumentVisibility.PRIVATE_USER
                && !Objects.equals(document.ownerUserId(), newOwner);
    }

    private String selectChunkStrategy(UploadKnowledgeDocumentCommand command) {
        try {
            String preview = parser.parseStructured(command.fileName(), command.contentType(), command.content())
                    .plainText();
            if (preview.length() > 8000) preview = preview.substring(0, 8000);
            return strategySelector.select(command.category(), command.fileName(), preview).name();
        } catch (RuntimeException ignored) {
            return ChunkStrategy.GENERAL.name();
        }
    }

    private static void ensureReadable(CurrentActor actor, KnowledgeDocument document) {
        if (ActorAccess.canManageTenant(actor)) {
            return;
        }
        if (document.visibility() != DocumentVisibility.TENANT
                && (document.ownerUserId() == null || !document.ownerUserId().equals(actor.userId()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (document.effectiveAudienceRoles().stream().noneMatch(actor.roles()::contains)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static Set<RoleCode> resolveAudienceRoles(CurrentActor actor, Long tenantId,
                                                       DocumentVisibility visibility,
                                                       List<RoleCode> requested) {
        Set<RoleCode> allowed = tenantId == null
                ? Set.of(RoleCode.PLATFORM_ADMIN, RoleCode.GYM_ADMIN, RoleCode.COACH, RoleCode.MEMBER)
                : Set.of(RoleCode.GYM_ADMIN, RoleCode.COACH, RoleCode.MEMBER);
        if (requested == null) {
            if (tenantId == null) return new LinkedHashSet<>(allowed);
            if (visibility == DocumentVisibility.PRIVATE_USER) {
                Set<RoleCode> ownerRoles = new LinkedHashSet<>(actor.roles());
                ownerRoles.retainAll(allowed);
                if (!ownerRoles.isEmpty()) return ownerRoles;
            }
            return new LinkedHashSet<>(allowed);
        }
        if (requested.isEmpty()) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        Set<RoleCode> result = new LinkedHashSet<>(requested);
        if (!allowed.containsAll(result)) throw new BusinessException(ErrorCode.FORBIDDEN);
        return result;
    }
}
