package com.gymmind.knowledge.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.knowledge.domain.model.DocumentStatus;
import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import com.gymmind.knowledge.infrastructure.parser.Chunk;
import com.gymmind.knowledge.infrastructure.parser.normalize.DocumentMarkdownNormalizer;
import com.gymmind.knowledge.infrastructure.search.KnowledgeIndexingService;
import com.gymmind.shared.async.JobStatus;
import com.gymmind.shared.async.PersistentJob;
import com.gymmind.shared.async.PersistentJobQueue;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
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

class KnowledgeIngestMarkdownWorkerTest {
    private static final Instant NOW = Instant.parse("2026-10-13T00:00:00Z");

    @Test
    void parseJobPersistsNormalizedMarkdownForHtml() {
        PersistentJobQueue jobs = mock(PersistentJobQueue.class);
        KnowledgeDocumentRepository documents = mock(KnowledgeDocumentRepository.class);
        InMemoryStore store = new InMemoryStore();
        KnowledgeIndexingService indexing = mock(KnowledgeIndexingService.class);
        KnowledgeChunkArtifactService artifacts = new KnowledgeChunkArtifactService(store, new ObjectMapper());
        KnowledgeDocument document = document(7L, 3L, "guide.html", "text/html");
        store.put(document.objectKey(), "<h1>标题</h1>".getBytes(StandardCharsets.UTF_8), "text/html");
        when(jobs.claimNext(NOW, java.time.Duration.ofMinutes(5)))
                .thenReturn(Optional.of(job(KnowledgeIngestWorker.JOB_TYPE_PARSE + ":3", 0)));
        when(documents.findForUpdateByTenantIdAndId(7L, 3L)).thenReturn(Optional.of(document));
        when(documents.save(any(KnowledgeDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(indexing.chunkFromBytes(eq(3L), eq("content.md"), eq("text/markdown"), any(), any(), any()))
                .thenReturn(List.of(new Chunk("c1", "3", 0, "标题")));

        KnowledgeIngestWorker worker = new KnowledgeIngestWorker(jobs, documents, store, indexing, artifacts,
                new KnowledgeIngestSettings(true), new KnowledgeIngestJobProperties(5, 30, 5),
                DocumentMarkdownNormalizer.create(true), Clock.fixed(NOW, ZoneOffset.UTC));
        worker.processNext();

        assertThat(document.normalizedMarkdownKey()).endsWith("normalized/content.md");
        assertThat(new String(store.read(document.normalizedMarkdownKey()), StandardCharsets.UTF_8)).contains("标题");
        assertThat(document.status()).isEqualTo(DocumentStatus.CHUNKED);
    }

    private static PersistentJob job(String type, int attempts) {
        return new PersistentJob(99L, type, 7L, JobStatus.RUNNING, attempts, NOW, NOW.plusSeconds(300), null);
    }

    private static KnowledgeDocument document(long tenantId, long id, String fileName, String contentType) {
        KnowledgeDocument document = KnowledgeDocument.create(tenantId, null, fileName, contentType,
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
