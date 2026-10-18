package com.gymmind.knowledge.application;

import com.gymmind.knowledge.domain.model.DocumentStatus;
import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.TenantContextHolder;
import com.gymmind.iam.domain.model.RoleCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.gymmind.knowledge.domain.model.KnowledgeType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class KnowledgeDocumentUseCaseTest {
    private final InMemoryKnowledgeDocumentRepository repository = new InMemoryKnowledgeDocumentRepository();
    private final RecordingObjectStore objectStore = new RecordingObjectStore();
    private final KnowledgeDocumentUseCase useCase = new DefaultKnowledgeDocumentUseCase(repository, objectStore);
    private final CurrentActor admin = new CurrentActor(11L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of("knowledge:write"), 0, "t1");
    private final CurrentActor member = new CurrentActor(12L, 7L, Set.of(RoleCode.MEMBER), Set.of("knowledge:read"), 0, "t2");
    private final CurrentActor otherTenant = new CurrentActor(13L, 8L, Set.of(RoleCode.GYM_ADMIN), Set.of("knowledge:write"), 0, "t3");

    @Test
    void platformUploadRemainsUploadingUntilAsyncIngestCompletes() {
        var indexing = mock(com.gymmind.knowledge.infrastructure.search.KnowledgeIndexingService.class);
        var worker = mock(KnowledgeIngestWorker.class);
        var asyncUseCase = new DefaultKnowledgeDocumentUseCase(repository, objectStore, indexing, worker,
                com.gymmind.tenancy.application.TenantQuotaServices.unboundedForLegacyConstruction());
        var platformAdmin = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0, "platform");

        KnowledgeDocumentView view = asyncUseCase.upload(platformAdmin,
                new UploadKnowledgeDocumentCommand("platform.txt", "text/plain", "platform".getBytes(), DocumentVisibility.TENANT));

        assertThat(view.status()).isEqualTo(DocumentStatus.UPLOADING);
        verify(worker).enqueue(null, view.id());
    }

    @Test
    void uploadValidPdfCreatesUploadingDocumentWithTenantObjectKey() {
        KnowledgeDocumentView view = useCase.upload(admin,
                new UploadKnowledgeDocumentCommand("guide.pdf", "application/pdf", pdf(), DocumentVisibility.TENANT));

        assertThat(view.status()).isEqualTo(DocumentStatus.UPLOADING);
        assertThat(view.visibility()).isEqualTo(DocumentVisibility.TENANT);
        assertThat(view.objectKey()).startsWith("tenant/7/knowledge/").endsWith("/guide.pdf");
        assertThat(objectStore.keys).containsExactly(view.objectKey());
    }

    @Test
    void uploadStoresSelectedAudienceRoles() {
        KnowledgeDocumentView view = useCase.upload(admin,
                new UploadKnowledgeDocumentCommand("guide.txt", "text/plain", "guide".getBytes(),
                        DocumentVisibility.TENANT, "GENERAL", List.of(RoleCode.COACH, RoleCode.MEMBER)));

        assertThat(view.audienceRoles()).containsExactly(RoleCode.COACH, RoleCode.MEMBER);
    }

    @Test
    void replacementIncrementsVersionAndKeepsKnowledgeKey() {
        KnowledgeDocumentView first = useCase.upload(admin, new UploadKnowledgeDocumentCommand("price.txt", "text/plain",
                "月卡100".getBytes(), DocumentVisibility.TENANT, "GENERAL", null, null, KnowledgeType.BASE_PRICE,
                "月卡", new BigDecimal("100"), null, null, null, null, null, 0, null));
        KnowledgeDocument old = repository.findById(first.id()).orElseThrow();
        old.markParsing(); old.markReady(); old.publish();
        KnowledgeDocumentView second = useCase.upload(admin, new UploadKnowledgeDocumentCommand("price2.txt", "text/plain",
                "月卡90".getBytes(), DocumentVisibility.TENANT, "GENERAL", null, first.id(), KnowledgeType.BASE_PRICE,
                "月卡", new BigDecimal("90"), null, null, null, null, null, 0, null));
        assertThat(second.knowledgeKey()).isEqualTo("document-" + first.id());
        assertThat(second.knowledgeVersion()).isEqualTo(2);
    }

    @Test
    void promotionRequiresValidityAndSupportsPercent() {
        assertThatThrownBy(() -> useCase.upload(admin, new UploadKnowledgeDocumentCommand("sale.txt", "text/plain",
                "9折".getBytes(), DocumentVisibility.TENANT, "GENERAL", null, null, KnowledgeType.PROMOTION_RULE,
                "月卡", null, "PERCENT", new BigDecimal("1.2"), LocalDateTime.now().minusDays(1),
                LocalDateTime.now(), null, 0, null))).isInstanceOf(BusinessException.class);
    }

    @Test
    void gymAdminCannotTargetPlatformAdministratorRole() {
        assertThatThrownBy(() -> useCase.upload(admin,
                new UploadKnowledgeDocumentCommand("guide.txt", "text/plain", "guide".getBytes(),
                        DocumentVisibility.TENANT, "GENERAL", List.of(RoleCode.PLATFORM_ADMIN))))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void platformDocumentDefaultsToAllAudienceRoles() {
        var platformAdmin = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0, "platform");
        KnowledgeDocumentView view = useCase.upload(platformAdmin,
                new UploadKnowledgeDocumentCommand("platform.txt", "text/plain", "platform".getBytes(), DocumentVisibility.TENANT));

        assertThat(view.audienceRoles()).containsExactly(RoleCode.COACH, RoleCode.GYM_ADMIN, RoleCode.MEMBER, RoleCode.PLATFORM_ADMIN);
    }

    @Test
    void ordinaryDocumentListHonorsAudienceRoles() {
        useCase.upload(admin, new UploadKnowledgeDocumentCommand("coach.txt", "text/plain", "coach".getBytes(),
                DocumentVisibility.TENANT, "GENERAL", List.of(RoleCode.COACH)));

        assertThat(useCase.list(member)).isEmpty();
    }

    @Test
    void privateDocumentIsVisibleOnlyToUploaderWithinSameTenant() {
        KnowledgeDocumentView view = useCase.upload(admin,
                new UploadKnowledgeDocumentCommand("notes.txt", "text/plain", "private".getBytes(), DocumentVisibility.PRIVATE_USER));

        assertThat(useCase.canRead(member, view.id())).isFalse();
        assertThat(useCase.canRead(admin, view.id())).isTrue();
        assertThat(useCase.canRead(otherTenant, view.id())).isFalse();
    }

    @Test
    void platformAdminListsAllDocumentsWithoutTenantContext() {
        useCase.upload(admin, new UploadKnowledgeDocumentCommand("guide.md", "text/markdown", "# guide".getBytes(), DocumentVisibility.TENANT));
        var otherTenantAdmin = new CurrentActor(13L, 8L, Set.of(RoleCode.GYM_ADMIN), Set.of("knowledge:write"), 0, "t3");
        useCase.upload(otherTenantAdmin, new UploadKnowledgeDocumentCommand("other.md", "text/markdown", "# other".getBytes(), DocumentVisibility.TENANT));
        var platformAdmin = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0, "platform");
        var platformDocument = useCase.upload(platformAdmin, new UploadKnowledgeDocumentCommand("platform.md", "text/markdown", "# platform".getBytes(), DocumentVisibility.TENANT));

        assertThat(useCase.list(platformAdmin)).hasSize(3);
        assertThat(platformDocument.tenantId()).isNull();
        assertThat(platformDocument.objectKey()).startsWith("platform/knowledge/");
        assertThat(TenantContextHolder.get()).isNull();
    }

    @Test
    void platformAdminListsOnlyContextTenantAndPlatformDocuments() {
        useCase.upload(admin, new UploadKnowledgeDocumentCommand("guide.md", "text/markdown", "# guide".getBytes(), DocumentVisibility.TENANT));
        useCase.upload(otherTenant, new UploadKnowledgeDocumentCommand("other.md", "text/markdown", "# other".getBytes(), DocumentVisibility.TENANT));
        var platformAdmin = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0, "platform");
        useCase.upload(platformAdmin, new UploadKnowledgeDocumentCommand("platform.md", "text/markdown", "# platform".getBytes(), DocumentVisibility.TENANT));

        TenantContextHolder.set(7L);
        try {
            assertThat(useCase.list(platformAdmin))
                    .extracting(KnowledgeDocumentView::fileName)
                    .containsExactlyInAnyOrder("guide.md", "platform.md");
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void platformAdminCannotModifyTenantDocument() {
        var document = useCase.upload(admin,
                new UploadKnowledgeDocumentCommand("guide.md", "text/markdown", "# guide".getBytes(), DocumentVisibility.TENANT));
        var platformAdmin = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0, "platform");

        assertThatThrownBy(() -> useCase.update(platformAdmin, document.id(),
                new UpdateKnowledgeDocumentCommand(DocumentVisibility.TENANT)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        assertThatThrownBy(() -> useCase.publish(platformAdmin, document.id(), false))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        assertThatThrownBy(() -> useCase.delete(platformAdmin, document.id()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void platformAdminCannotReplaceTenantDocumentContent() {
        KnowledgeDocumentView document = useCase.upload(admin,
                new UploadKnowledgeDocumentCommand("guide.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "docx".getBytes(), DocumentVisibility.TENANT));
        var platformAdmin = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0, "platform");

        assertThatThrownBy(() -> useCase.replaceBinaryContent(platformAdmin, document.id(), "changed".getBytes()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void publishedDocumentIsNotReportedAsPublishedWhileBeingReindexed() {
        KnowledgeDocumentView uploaded = useCase.upload(admin,
                new UploadKnowledgeDocumentCommand("guide.md", "text/markdown", "# guide".getBytes(), DocumentVisibility.TENANT));
        KnowledgeDocument document = repository.findByTenantIdAndId(7L, uploaded.id()).orElseThrow();
        document.markParsing();
        document.markReady();

        assertThat(useCase.publish(admin, uploaded.id(), true).published()).isTrue();

        document.markParsing();
        assertThat(useCase.list(admin)).singleElement().extracting(KnowledgeDocumentView::published)
                .isEqualTo(false);

        document.markReady();
        assertThat(useCase.list(admin)).singleElement().extracting(KnowledgeDocumentView::published)
                .isEqualTo(true);
    }

    @Test
    void gymAdminCanUpdateDocumentVisibilityWithinTenant() {
        KnowledgeDocumentView view = useCase.upload(admin,
                new UploadKnowledgeDocumentCommand("guide.md", "text/markdown", "# guide".getBytes(), DocumentVisibility.TENANT));
        KnowledgeDocumentView updated = useCase.update(admin, view.id(),
                new UpdateKnowledgeDocumentCommand(DocumentVisibility.PRIVATE_USER));
        assertThat(updated.visibility()).isEqualTo(DocumentVisibility.PRIVATE_USER);
    }

    @Test
    void tighteningVisibilityRemovesExistingIndexBeforeQueueingReindex() {
        var indexing = mock(com.gymmind.knowledge.infrastructure.search.KnowledgeIndexingService.class);
        var ingestWorker = mock(KnowledgeIngestWorker.class);
        var repository = new InMemoryKnowledgeDocumentRepository();
        var service = new DefaultKnowledgeDocumentUseCase(repository, new RecordingObjectStore(), indexing,
                ingestWorker, null);
        KnowledgeDocumentView view = service.upload(admin,
                new UploadKnowledgeDocumentCommand("guide.md", "text/markdown", "# guide".getBytes(), DocumentVisibility.TENANT));
        KnowledgeDocument document = repository.findByTenantIdAndId(7L, view.id()).orElseThrow();
        document.markParsing();
        document.markReady();
        reset(indexing, ingestWorker);

        service.update(admin, view.id(), new UpdateKnowledgeDocumentCommand(DocumentVisibility.PRIVATE_USER));

        var order = inOrder(indexing, ingestWorker);
        order.verify(indexing).deleteDocument(7L, view.id());
        order.verify(ingestWorker).enqueue(7L, view.id());
    }

    @Test
    void changingAudienceRolesRemovesExistingIndexBeforeQueueingReindex() {
        var indexing = mock(com.gymmind.knowledge.infrastructure.search.KnowledgeIndexingService.class);
        var ingestWorker = mock(KnowledgeIngestWorker.class);
        var repository = new InMemoryKnowledgeDocumentRepository();
        var service = new DefaultKnowledgeDocumentUseCase(repository, new RecordingObjectStore(), indexing,
                ingestWorker, null);
        KnowledgeDocumentView view = service.upload(admin,
                new UploadKnowledgeDocumentCommand("guide.md", "text/markdown", "# guide".getBytes(),
                        DocumentVisibility.TENANT, "GENERAL", List.of(RoleCode.COACH)));
        reset(indexing, ingestWorker);

        service.update(admin, view.id(), new UpdateKnowledgeDocumentCommand(DocumentVisibility.TENANT, List.of(RoleCode.MEMBER)));

        var order = inOrder(indexing, ingestWorker);
        order.verify(indexing).deleteDocument(7L, view.id());
        order.verify(ingestWorker).enqueue(7L, view.id());
    }

    @Test
    void listRequiresReadPermissionAndReturnsTenantDocuments() {
        useCase.upload(admin, new UploadKnowledgeDocumentCommand("guide.md", "text/markdown", "# guide".getBytes(), DocumentVisibility.TENANT));
        var reader = new CurrentActor(12L, 7L, Set.of(RoleCode.MEMBER), Set.of("knowledge:read"), 0, "t2");
        assertThat(useCase.list(reader)).hasSize(1);
        assertThat(useCase.list(otherTenant)).isEmpty();
    }

    @Test
    void uploadRejectsOversizedAndInvalidMagicContent() {
        byte[] oversized = new byte[KnowledgeDocumentValidator.MAX_BYTES + 1];
        assertThatThrownBy(() -> useCase.upload(admin,
                new UploadKnowledgeDocumentCommand("guide.pdf", "application/pdf", oversized, DocumentVisibility.TENANT)))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);
        assertThatThrownBy(() -> useCase.upload(admin,
                new UploadKnowledgeDocumentCommand("guide.pdf", "application/pdf", "not pdf".getBytes(), DocumentVisibility.TENANT)))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void deleteRejectsCrossTenantAccess() {
        KnowledgeDocumentView view = useCase.upload(admin,
                new UploadKnowledgeDocumentCommand("guide.md", "text/markdown", "# guide".getBytes(), DocumentVisibility.TENANT));
        assertThatThrownBy(() -> useCase.delete(otherTenant, view.id()))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(useCase.delete(admin, view.id()).status()).isEqualTo(DocumentStatus.DELETED);
    }

    private static byte[] pdf() { return "%PDF-1.7\nbody".getBytes(); }

    private static final class RecordingObjectStore implements KnowledgeObjectStore {
        private final java.util.List<String> keys = new java.util.ArrayList<>();
        public void put(String key, byte[] content, String contentType) { keys.add(key); }
        public void delete(String key) { keys.remove(key); }
    }

    private static final class InMemoryKnowledgeDocumentRepository implements KnowledgeDocumentRepository {
        private long sequence = 0;
        private final java.util.Map<Long, KnowledgeDocument> values = new java.util.HashMap<>();
        public KnowledgeDocument save(KnowledgeDocument document) { if (document.id() == null) document.assignId(++sequence); values.put(document.id(), document); return document; }
        public Optional<KnowledgeDocument> findByTenantIdAndId(Long tenantId, Long id) {
            return Optional.ofNullable(values.get(id)).filter(d -> tenantId.equals(d.tenantId()));
        }

        public Optional<KnowledgeDocument> findById(Long id) {
            return Optional.ofNullable(values.get(id));
        }

        public Optional<KnowledgeDocument> findForUpdateById(Long id) {
            return findById(id);
        }

        public List<KnowledgeDocument> findByTenantId(Long tenantId) {
            return values.values().stream().filter(d -> tenantId.equals(d.tenantId())).toList();
        }
        public List<KnowledgeDocument> findByKnowledgeKey(Long tenantId, String key) {
            return values.values().stream().filter(d -> java.util.Objects.equals(tenantId, d.tenantId()) && java.util.Objects.equals(key, d.knowledgeKey())).toList();
        }

        public List<KnowledgeDocument> findAllDocuments() {
            return values.values().stream().sorted((left, right) -> Long.compare(right.id(), left.id())).toList();
        }

    }
}
