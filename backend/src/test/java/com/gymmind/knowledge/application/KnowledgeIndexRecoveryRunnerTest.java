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

        new KnowledgeIndexRecoveryRunner(documents, worker, false).run(mock(ApplicationArguments.class));

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

        new KnowledgeIndexRecoveryRunner(documents, worker, false).run(mock(ApplicationArguments.class));

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

        new KnowledgeIndexRecoveryRunner(documents, worker, true).run(mock(ApplicationArguments.class));

        verify(worker).enqueue(null, 9L);
    }

    private static KnowledgeDocument platformDocument(long id) {
        KnowledgeDocument document = KnowledgeDocument.create(null, null, "guide.txt", "text/plain",
                DocumentVisibility.TENANT, "recovery-test");
        document.assignId(id);
        return document;
    }
}
