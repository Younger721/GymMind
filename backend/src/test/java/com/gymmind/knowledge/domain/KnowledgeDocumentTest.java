package com.gymmind.knowledge.domain;

import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.knowledge.domain.model.DocumentStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class KnowledgeDocumentTest {
    @Test void tenantDocumentUsesTenantObjectKey() {
        var d = KnowledgeDocument.create(7L, null, "guide.pdf", "application/pdf", DocumentVisibility.TENANT, 42L);
        assertThat(d.objectKey()).isEqualTo("tenant/7/knowledge/42/guide.pdf");
        assertThat(d.status()).isEqualTo(DocumentStatus.UPLOADING);
    }

    @Test void privateDocumentRequiresOwnerAndCanBeTombstoned() {
        var d = KnowledgeDocument.create(7L, 11L, "notes.txt", "text/plain", DocumentVisibility.PRIVATE_USER, 43L);
        assertThat(d.ownerUserId()).isEqualTo(11L);
        d.markReady();
        d.tombstone();
        assertThat(d.status()).isEqualTo(DocumentStatus.DELETED);
    }

    @Test void rejectsInvalidVisibilityOwnerAndTransitions() {
        assertThatThrownBy(() -> KnowledgeDocument.create(7L, null, "x", "text/plain", DocumentVisibility.PRIVATE_USER, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        var d = KnowledgeDocument.create(7L, null, "x", "text/plain", DocumentVisibility.TENANT, 1L);
        d.markParsing();
        d.markReady();
        assertThat(d.status()).isEqualTo(DocumentStatus.READY);
    }

    @Test void readyDocumentCanBeMarkedForReindexing() {
        var d = KnowledgeDocument.create(7L, null, "x", "text/plain", DocumentVisibility.TENANT, 1L);
        d.markParsing();
        d.markReady();

        d.markParsing();

        assertThat(d.status()).isEqualTo(DocumentStatus.PARSING);
    }

    @Test void objectKeyUsesOnlySafeBaseFileName() {
        var d = KnowledgeDocument.create(7L, null, "../private\\secret.txt", "text/plain",
                DocumentVisibility.TENANT, 1L);

        assertThat(d.fileName()).isEqualTo("secret.txt");
        assertThat(d.objectKey()).isEqualTo("tenant/7/knowledge/1/secret.txt");
    }

    @Test void newDocumentsExposeGeneralChunkStrategyVersionOne() {
        var d = KnowledgeDocument.create(7L, null, "guide.txt", "text/plain", DocumentVisibility.TENANT, 1L);

        assertThat(d.chunkStrategy()).isEqualTo("GENERAL");
        assertThat(d.chunkStrategyVersion()).isEqualTo(1);
    }

    @Test void chunkStrategyCanBeUpdatedAndNormalizesItsName() {
        var d = KnowledgeDocument.create(7L, null, "guide.txt", "text/plain", DocumentVisibility.TENANT, 1L);

        d.updateChunkStrategy(" procedure ", 2);

        assertThat(d.chunkStrategy()).isEqualTo("PROCEDURE");
        assertThat(d.chunkStrategyVersion()).isEqualTo(2);
    }

    @Test void chunkStrategyRejectsBlankNamesAndNonPositiveVersions() {
        var d = KnowledgeDocument.create(7L, null, "guide.txt", "text/plain", DocumentVisibility.TENANT, 1L);

        assertThatThrownBy(() -> d.updateChunkStrategy(" ", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> d.updateChunkStrategy("GENERAL", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
