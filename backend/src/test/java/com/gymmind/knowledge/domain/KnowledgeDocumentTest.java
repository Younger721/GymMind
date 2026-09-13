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
}
