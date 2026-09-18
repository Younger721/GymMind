package com.gymmind.knowledge.application;

import com.gymmind.knowledge.domain.model.DocumentStatus;
import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.iam.domain.model.RoleCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeDocumentUseCaseTest {
    private final InMemoryKnowledgeDocumentRepository repository = new InMemoryKnowledgeDocumentRepository();
    private final RecordingObjectStore objectStore = new RecordingObjectStore();
    private final KnowledgeDocumentUseCase useCase = new DefaultKnowledgeDocumentUseCase(repository, objectStore);
    private final CurrentActor admin = new CurrentActor(11L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of("knowledge:write"), 0, "t1");
    private final CurrentActor member = new CurrentActor(12L, 7L, Set.of(RoleCode.MEMBER), Set.of("knowledge:read"), 0, "t2");
    private final CurrentActor otherTenant = new CurrentActor(13L, 8L, Set.of(RoleCode.GYM_ADMIN), Set.of("knowledge:write"), 0, "t3");

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
    void privateDocumentIsVisibleOnlyToUploaderWithinSameTenant() {
        KnowledgeDocumentView view = useCase.upload(admin,
                new UploadKnowledgeDocumentCommand("notes.txt", "text/plain", "private".getBytes(), DocumentVisibility.PRIVATE_USER));

        assertThat(useCase.canRead(member, view.id())).isFalse();
        assertThat(useCase.canRead(admin, view.id())).isTrue();
        assertThat(useCase.canRead(otherTenant, view.id())).isFalse();
    }

    @Test
    void platformAdminWithWhitelistCanListAllTenantDocuments() {
        useCase.upload(admin, new UploadKnowledgeDocumentCommand("guide.md", "text/markdown", "# guide".getBytes(), DocumentVisibility.TENANT));
        var platformAdmin = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN),
                Set.of("platform:knowledge:read"), 0, "platform");
        assertThat(useCase.list(platformAdmin)).hasSize(1);

        var otherTenantAdmin = new CurrentActor(13L, 8L, Set.of(RoleCode.GYM_ADMIN), Set.of("knowledge:write"), 0, "t3");
        useCase.upload(otherTenantAdmin, new UploadKnowledgeDocumentCommand("other.md", "text/markdown", "# other".getBytes(), DocumentVisibility.TENANT));
        assertThat(useCase.list(platformAdmin)).hasSize(2);
    }

    @Test
    void listRequiresReadPermissionAndReturnsTenantDocuments() {
        useCase.upload(admin, new UploadKnowledgeDocumentCommand("guide.md", "text/markdown", "# guide".getBytes(), DocumentVisibility.TENANT));
        var reader = new CurrentActor(12L, 7L, Set.of(RoleCode.MEMBER), Set.of("knowledge:read"), 0, "t2");
        assertThat(useCase.list(reader)).hasSize(1);
        assertThatThrownBy(() -> useCase.list(otherTenant)).isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
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
    void reindexAndDeleteRejectCrossTenantAccessAndFollowStatusTransitions() {
        KnowledgeDocumentView view = useCase.upload(admin,
                new UploadKnowledgeDocumentCommand("guide.md", "text/markdown", "# guide".getBytes(), DocumentVisibility.TENANT));
        assertThat(useCase.reindex(admin, view.id()).status()).isEqualTo(DocumentStatus.PARSING);
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

        public List<KnowledgeDocument> findByTenantId(Long tenantId) {
            return values.values().stream().filter(d -> tenantId.equals(d.tenantId())).toList();
        }

        public List<KnowledgeDocument> findAllAccessible() {
            return values.values().stream()
                    .filter(d -> d.status() != DocumentStatus.DELETED)
                    .toList();
        }
    }
}
