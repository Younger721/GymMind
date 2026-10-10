package com.gymmind.knowledge.application;

import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

import java.util.List;

import static org.mockito.Mockito.*;

class KnowledgeIndexRecoveryRunnerTest {

    @Test
    void requeuesDocumentsLeftParsingAfterRestart() {
        KnowledgeDocumentRepository documents = mock(KnowledgeDocumentRepository.class);
        KnowledgeIngestWorker worker = mock(KnowledgeIngestWorker.class);
        KnowledgeDocument document = platformDocument(7L);
        document.markParsing();
        when(documents.findAllDocuments()).thenReturn(List.of(document));

        new KnowledgeIndexRecoveryRunner(documents, worker, KnowledgeIngestSettings.disabled(), false)
                .run(mock(ApplicationArguments.class));

        verify(worker).enqueue(null, 7L);
    }

    @Test
    void doesNotRequeuePublishedReadyDocumentsOnEveryRestart() {
        KnowledgeDocumentRepository documents = mock(KnowledgeDocumentRepository.class);
        KnowledgeIngestWorker worker = mock(KnowledgeIngestWorker.class);
        KnowledgeDocument document = platformDocument(8L);
        document.markParsing();
        document.markReady();
        document.publish();
        when(documents.findAllDocuments()).thenReturn(List.of(document));

        new KnowledgeIndexRecoveryRunner(documents, worker, KnowledgeIngestSettings.disabled(), false)
                .run(mock(ApplicationArguments.class));

        verify(worker, never()).enqueue(any(), eq(8L));
    }

    @Test
    void requeuesPublishedReadyDocumentsWhenRecoveryIsEnabled() {
        KnowledgeDocumentRepository documents = mock(KnowledgeDocumentRepository.class);
        KnowledgeIngestWorker worker = mock(KnowledgeIngestWorker.class);
        KnowledgeDocument document = platformDocument(9L);
        document.markParsing();
        document.markReady();
        document.publish();
        when(documents.findAllDocuments()).thenReturn(List.of(document));

        new KnowledgeIndexRecoveryRunner(documents, worker, KnowledgeIngestSettings.disabled(), true)
                .run(mock(ApplicationArguments.class));

        verify(worker).enqueue(null, 9L);
    }

    @Test
    void requeuesChunkedDocumentsToIndexJobWhenTwoPhaseEnabled() {
        KnowledgeDocumentRepository documents = mock(KnowledgeDocumentRepository.class);
        KnowledgeIngestWorker worker = mock(KnowledgeIngestWorker.class);
        KnowledgeDocument document = tenantDocument(7L, 10L);
        document.markParsing();
        document.assignChunkArtifactKey("tenant/7/knowledge/x/chunks/GENERAL-v1-1.json");
        document.markChunked();
        when(documents.findAllDocuments()).thenReturn(List.of(document));

        new KnowledgeIndexRecoveryRunner(documents, worker, new KnowledgeIngestSettings(true), false)
                .run(mock(ApplicationArguments.class));

        verify(worker).enqueueIndex(7L, 10L);
        verify(worker, never()).enqueue(any(), eq(10L));
    }

    private static KnowledgeDocument platformDocument(long id) {
        KnowledgeDocument document = KnowledgeDocument.create(null, null, "guide.txt", "text/plain",
                DocumentVisibility.TENANT, "recovery-test");
        document.assignId(id);
        return document;
    }

    private static KnowledgeDocument tenantDocument(long tenantId, long id) {
        KnowledgeDocument document = KnowledgeDocument.create(tenantId, null, "guide.txt", "text/plain",
                DocumentVisibility.TENANT, "recovery-test");
        document.assignId(id);
        return document;
    }
}
