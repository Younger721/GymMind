package com.gymmind.knowledge.application;

import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import com.gymmind.knowledge.infrastructure.search.KnowledgeIndexingService;
import com.gymmind.shared.async.JobStatus;
import com.gymmind.shared.async.PersistentJob;
import com.gymmind.shared.async.PersistentJobQueue;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KnowledgeIngestWorkerTest {
    private static final Instant NOW = Instant.parse("2026-09-21T00:00:00Z");

    @Test
    void missingObjectRetriesAndMarksDocumentFailedAfterFinalAttempt() {
        PersistentJobQueue jobs = mock(PersistentJobQueue.class);
        KnowledgeDocumentRepository documents = mock(KnowledgeDocumentRepository.class);
        KnowledgeObjectStore store = mock(KnowledgeObjectStore.class);
        KnowledgeIndexingService indexing = mock(KnowledgeIndexingService.class);
        KnowledgeDocument document = document(7L, 3L);
        when(jobs.claimNext(NOW, Duration.ofMinutes(5))).thenReturn(Optional.of(job(4)));
        when(documents.findForUpdateByTenantIdAndId(7L, 3L)).thenReturn(Optional.of(document));
        when(store.read(document.objectKey())).thenReturn(null);
        when(documents.save(any(KnowledgeDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        new KnowledgeIngestWorker(jobs, documents, store, indexing, Clock.fixed(NOW, ZoneOffset.UTC)).processNext();

        assertThat(document.status()).isEqualTo(com.gymmind.knowledge.domain.model.DocumentStatus.FAILED);
        verify(jobs).retry(eq(99L), anyString(), eq(NOW));
        verify(jobs, never()).complete(99L);
    }

    @Test
    void missingObjectKeepsDocumentParsingWhileRetryAttemptsRemain() {
        PersistentJobQueue jobs = mock(PersistentJobQueue.class);
        KnowledgeDocumentRepository documents = mock(KnowledgeDocumentRepository.class);
        KnowledgeObjectStore store = mock(KnowledgeObjectStore.class);
        KnowledgeIndexingService indexing = mock(KnowledgeIndexingService.class);
        KnowledgeDocument document = document(7L, 3L);
        when(jobs.claimNext(NOW, Duration.ofMinutes(5))).thenReturn(Optional.of(job(0)));
        when(documents.findForUpdateByTenantIdAndId(7L, 3L)).thenReturn(Optional.of(document));
        when(store.read(document.objectKey())).thenReturn(null);
        when(documents.save(any(KnowledgeDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        new KnowledgeIngestWorker(jobs, documents, store, indexing, Clock.fixed(NOW, ZoneOffset.UTC)).processNext();

        assertThat(document.status()).isEqualTo(com.gymmind.knowledge.domain.model.DocumentStatus.PARSING);
        verify(jobs).retry(eq(99L), anyString(), eq(NOW));
        verify(jobs, never()).complete(99L);
    }

    @Test
    void retryCanProcessDocumentThatIsAlreadyParsing() {
        PersistentJobQueue jobs = mock(PersistentJobQueue.class);
        KnowledgeDocumentRepository documents = mock(KnowledgeDocumentRepository.class);
        KnowledgeObjectStore store = mock(KnowledgeObjectStore.class);
        KnowledgeIndexingService indexing = mock(KnowledgeIndexingService.class);
        KnowledgeDocument document = document(7L, 3L);
        when(jobs.claimNext(NOW, Duration.ofMinutes(5)))
                .thenReturn(Optional.of(job(0)), Optional.of(job(1)));
        when(documents.findForUpdateByTenantIdAndId(7L, 3L)).thenReturn(Optional.of(document));
        when(store.read(document.objectKey())).thenReturn(null);
        when(documents.save(any(KnowledgeDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        KnowledgeIngestWorker worker = new KnowledgeIngestWorker(
                jobs, documents, store, indexing, Clock.fixed(NOW, ZoneOffset.UTC));

        worker.processNext();
        worker.processNext();

        assertThat(document.status()).isEqualTo(com.gymmind.knowledge.domain.model.DocumentStatus.PARSING);
        verify(jobs, times(2)).retry(eq(99L), anyString(), eq(NOW));
    }

    @Test
    void queuedIngestSkipsDocumentThatWasAlreadyDeleted() {
        PersistentJobQueue jobs = mock(PersistentJobQueue.class);
        KnowledgeDocumentRepository documents = mock(KnowledgeDocumentRepository.class);
        KnowledgeObjectStore store = mock(KnowledgeObjectStore.class);
        KnowledgeIndexingService indexing = mock(KnowledgeIndexingService.class);
        KnowledgeDocument document = document(7L, 3L);
        document.tombstone();
        when(jobs.claimNext(NOW, Duration.ofMinutes(5))).thenReturn(Optional.of(job(0)));
        when(documents.findForUpdateByTenantIdAndId(7L, 3L)).thenReturn(Optional.of(document));

        new KnowledgeIngestWorker(jobs, documents, store, indexing, Clock.fixed(NOW, ZoneOffset.UTC)).processNext();

        verify(jobs).complete(99L);
        verify(jobs, never()).retry(anyLong(), anyString(), any());
        verify(indexing, never()).publish(any(), any(), any(), any(), any(), any());
        verify(documents, never()).save(any());
    }

    private static PersistentJob job(int attempts) {
        return new PersistentJob(99L, KnowledgeIngestWorker.JOB_TYPE + ":3", 7L, JobStatus.RUNNING,
                attempts, NOW, NOW.plusSeconds(300), null);
    }

    private static KnowledgeDocument document(long tenantId, long id) {
        KnowledgeDocument document = KnowledgeDocument.create(tenantId, null, "guide.txt", "text/plain",
                DocumentVisibility.TENANT, "stable-key");
        document.assignId(id);
        return document;
    }
}
