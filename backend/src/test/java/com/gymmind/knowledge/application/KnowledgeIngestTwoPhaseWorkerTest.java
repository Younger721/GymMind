package com.gymmind.knowledge.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.knowledge.domain.model.DocumentStatus;
import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import com.gymmind.knowledge.infrastructure.parser.Chunk;
import com.gymmind.knowledge.infrastructure.search.KnowledgeIndexingService;
import com.gymmind.shared.async.JobStatus;
import com.gymmind.shared.async.PersistentJob;
import com.gymmind.shared.async.PersistentJobQueue;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class KnowledgeIngestTwoPhaseWorkerTest {
    private static final Instant NOW = Instant.parse("2026-10-10T00:00:00Z");

    @Test
    void parseJobWritesArtifactAndEnqueuesIndex() {
        PersistentJobQueue jobs = mock(PersistentJobQueue.class);
        KnowledgeDocumentRepository documents = mock(KnowledgeDocumentRepository.class);
        KnowledgeObjectStore store = new InMemoryStore();
        KnowledgeIndexingService indexing = mock(KnowledgeIndexingService.class);
        KnowledgeChunkArtifactService artifacts = new KnowledgeChunkArtifactService(store, new ObjectMapper());
        KnowledgeDocument document = document(7L, 3L);
        store.put(document.objectKey(), "# Title\nbody".getBytes(), "text/markdown");
        when(jobs.claimNext(NOW, Duration.ofMinutes(5)))
                .thenReturn(Optional.of(job(KnowledgeIngestWorker.JOB_TYPE_PARSE + ":3", 0)));
        when(documents.findForUpdateByTenantIdAndId(7L, 3L)).thenReturn(Optional.of(document));
        when(documents.save(any(KnowledgeDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(indexing.chunkFromBytes(eq(3L), any(), any(), any(), any(), any()))
                .thenReturn(List.of(new Chunk("c1", "3", 0, "body text")));

        KnowledgeIngestWorker worker = new KnowledgeIngestWorker(jobs, documents, store, indexing, artifacts,
                new KnowledgeIngestSettings(true), Clock.fixed(NOW, ZoneOffset.UTC));
        worker.processNext();

        assertThat(document.status()).isEqualTo(DocumentStatus.CHUNKED);
        assertThat(document.chunkArtifactKey()).isNotBlank();
        verify(jobs).enqueue(eq(KnowledgeIngestWorker.JOB_TYPE_INDEX + ":3"), eq(7L));
        verify(jobs).complete(99L);
    }

    private static PersistentJob job(String type, int attempts) {
        return new PersistentJob(99L, type, 7L, JobStatus.RUNNING, attempts, NOW, NOW.plusSeconds(300), null);
    }

    private static KnowledgeDocument document(long tenantId, long id) {
        KnowledgeDocument document = KnowledgeDocument.create(tenantId, null, "guide.md", "text/markdown",
                DocumentVisibility.TENANT, "stable-key");
        document.assignId(id);
        return document;
    }

    private static final class InMemoryStore implements KnowledgeObjectStore {
        private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

        @Override
        public void put(String key, byte[] content, String contentType) {
            objects.put(key, content);
        }

        @Override
        public byte[] read(String key) {
            return objects.get(key);
        }

        @Override
        public void delete(String key) {
            objects.remove(key);
        }
    }
}
