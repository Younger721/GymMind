package com.gymmind.knowledge.application;

import com.gymmind.knowledge.domain.model.DocumentStatus;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import com.gymmind.knowledge.infrastructure.search.KnowledgeIndexingService;
import com.gymmind.shared.async.PersistentJobQueue;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class KnowledgeIngestWorker {

    public static final String JOB_TYPE = "KNOWLEDGE_INGEST";
    public static final String JOB_TYPE_PARSE = "KNOWLEDGE_INGEST_PARSE";
    public static final String JOB_TYPE_INDEX = "KNOWLEDGE_INGEST_INDEX";
    private final PersistentJobQueue jobs;
    private final KnowledgeDocumentRepository documents;
    private final KnowledgeObjectStore store;
    private final KnowledgeIndexingService indexing;
    private final KnowledgeChunkArtifactService artifacts;
    private final KnowledgeIngestSettings settings;
    private final KnowledgeIngestJobProperties jobProperties;
    private final Clock clock;

    public KnowledgeIngestWorker(
            PersistentJobQueue jobs,
            KnowledgeDocumentRepository documents,
            KnowledgeObjectStore store,
            KnowledgeIndexingService indexing,
            Clock clock) {
        this(jobs, documents, store, indexing, null, KnowledgeIngestSettings.disabled(), defaultJobProperties(), clock);
    }

    public KnowledgeIngestWorker(
            PersistentJobQueue jobs,
            KnowledgeDocumentRepository documents,
            KnowledgeObjectStore store,
            KnowledgeIndexingService indexing,
            KnowledgeChunkArtifactService artifacts,
            KnowledgeIngestSettings settings,
            Clock clock) {
        this(jobs, documents, store, indexing, artifacts, settings, defaultJobProperties(), clock);
    }

    public KnowledgeIngestWorker(
            PersistentJobQueue jobs,
            KnowledgeDocumentRepository documents,
            KnowledgeObjectStore store,
            KnowledgeIndexingService indexing,
            KnowledgeChunkArtifactService artifacts,
            KnowledgeIngestSettings settings,
            KnowledgeIngestJobProperties jobProperties,
            Clock clock) {
        this.jobs = jobs;
        this.documents = documents;
        this.store = store;
        this.indexing = indexing;
        this.artifacts = artifacts;
        this.settings = settings == null ? KnowledgeIngestSettings.disabled() : settings;
        this.jobProperties = jobProperties == null ? defaultJobProperties() : jobProperties;
        this.clock = clock;
    }

    private static KnowledgeIngestJobProperties defaultJobProperties() {
        return new KnowledgeIngestJobProperties(5, 30, 5);
    }

    public void enqueue(Long tenantId, Long documentId) {
        if (documentId == null) {
            return;
        }
        if (settings.twoPhase()) {
            jobs.enqueue(JOB_TYPE_PARSE + ":" + documentId, tenantId);
        } else {
            jobs.enqueue(JOB_TYPE + ":" + documentId, tenantId);
        }
    }

    public void enqueueIndex(Long tenantId, Long documentId) {
        if (documentId == null) {
            return;
        }
        jobs.enqueue(JOB_TYPE_INDEX + ":" + documentId, tenantId);
    }

    @Scheduled(fixedDelayString = "${gymmind.knowledge.ingest-delay-ms:5000}")
    @Transactional
    public void processNext() {
        Instant now = clock.instant();
        jobs.recoverExpired(now);
        jobs.claimNext(now, jobProperties.leaseDuration()).ifPresent(job -> {
            if (job.type() == null || !isIngestJob(job.type())) {
                jobs.complete(job.id());
                return;
            }
            try {
                if (job.type().startsWith(JOB_TYPE_INDEX + ":")) {
                    processIndexJob(job, now);
                } else if (job.type().startsWith(JOB_TYPE_PARSE + ":")) {
                    processParseJob(job, now);
                } else if (job.type().startsWith(JOB_TYPE + ":")) {
                    processLegacyJob(job, now);
                } else {
                    jobs.complete(job.id());
                }
            } catch (RuntimeException ex) {
                handleFailure(job, ex, now);
            }
        });
    }

    private void processLegacyJob(com.gymmind.shared.async.PersistentJob job, Instant now) {
        KnowledgeDocument document = loadDocument(job);
        if (document == null || document.status() == DocumentStatus.DELETED) {
            jobs.complete(job.id());
            return;
        }
        document.markParsing();
        documents.save(document);
        byte[] bytes = requireSourceBytes(document);
        document.updateChunkStrategy(document.chunkStrategy(), document.chunkStrategyVersion());
        boolean published = isLegacyGeneral(document)
                ? indexing.publish(document.tenantId(), document.id(), document.ownerUserId(),
                document.fileName(), document.contentType(), bytes)
                : indexing.publish(document.tenantId(), document.id(), document.ownerUserId(),
                document.fileName(), document.contentType(), bytes,
                document.chunkStrategy(), document.chunkStrategyVersion(), document.knowledgeKey(), document.knowledgeVersion());
        if (!published) {
            throw new IllegalStateException("knowledge indexing is unavailable");
        }
        finalizeReady(document);
        jobs.complete(job.id());
    }

    private void processParseJob(com.gymmind.shared.async.PersistentJob job, Instant now) {
        if (artifacts == null) {
            throw new IllegalStateException("chunk artifact service is unavailable");
        }
        KnowledgeDocument document = loadDocument(job);
        if (document == null || document.status() == DocumentStatus.DELETED) {
            jobs.complete(job.id());
            return;
        }
        if (document.status() == DocumentStatus.READY) {
            jobs.complete(job.id());
            return;
        }
        if (document.status() == DocumentStatus.CHUNKED || document.status() == DocumentStatus.INDEXING) {
            enqueueIndex(document.tenantId(), document.id());
            jobs.complete(job.id());
            return;
        }
        document.markParsing();
        documents.save(document);
        byte[] bytes = requireSourceBytes(document);
        document.updateChunkStrategy(document.chunkStrategy(), document.chunkStrategyVersion());
        List<com.gymmind.knowledge.infrastructure.parser.Chunk> chunks = indexing.chunkFromBytes(
                document.id(), document.fileName(), document.contentType(), bytes,
                document.chunkStrategy(), document.chunkStrategyVersion());
        String artifactKey = artifacts.writeArtifact(document, chunks);
        document.assignChunkArtifactKey(artifactKey);
        document.markChunked();
        documents.save(document);
        enqueueIndex(document.tenantId(), document.id());
        jobs.complete(job.id());
    }

    private void processIndexJob(com.gymmind.shared.async.PersistentJob job, Instant now) {
        if (artifacts == null) {
            throw new IllegalStateException("chunk artifact service is unavailable");
        }
        KnowledgeDocument document = loadDocument(job);
        if (document == null || document.status() == DocumentStatus.DELETED) {
            jobs.complete(job.id());
            return;
        }
        if (document.status() == DocumentStatus.READY) {
            jobs.complete(job.id());
            return;
        }
        if (document.chunkArtifactKey() == null || document.chunkArtifactKey().isBlank()) {
            throw new IllegalStateException("chunk artifact key is missing");
        }
        document.markIndexing();
        documents.save(document);
        KnowledgeChunkArtifact artifact = artifacts.readArtifact(document.chunkArtifactKey());
        boolean published = indexing.publishFromArtifact(document.tenantId(), document.id(),
                document.ownerUserId(), artifact);
        if (!published) {
            throw new IllegalStateException("knowledge indexing is unavailable");
        }
        finalizeReady(document);
        jobs.complete(job.id());
    }

    private void finalizeReady(KnowledgeDocument document) {
        KnowledgeDocument current = document.tenantId() == null
                ? documents.findForUpdateById(document.id()).orElse(null)
                : documents.findForUpdateByTenantIdAndId(document.tenantId(), document.id()).orElse(null);
        if (current == null || current.status() == DocumentStatus.DELETED) {
            indexing.deleteDocument(document.tenantId(), document.id());
            return;
        }
        current.markReady();
        documents.save(current);
    }

    private KnowledgeDocument loadDocument(com.gymmind.shared.async.PersistentJob job) {
        Long documentId = parseDocumentId(job.type());
        return job.tenantId() == null
                ? documents.findForUpdateById(documentId).orElse(null)
                : documents.findForUpdateByTenantIdAndId(job.tenantId(), documentId).orElse(null);
    }

    private byte[] requireSourceBytes(KnowledgeDocument document) {
        byte[] bytes = store.read(document.objectKey());
        if (bytes == null) {
            throw new IllegalStateException("knowledge object is unavailable");
        }
        return bytes;
    }

    private void handleFailure(com.gymmind.shared.async.PersistentJob job, RuntimeException ex, Instant now) {
        KnowledgeDocument document = loadDocument(job);
        if (document != null && document.status() != DocumentStatus.DELETED) {
            try {
                if (job.attempts() + 1 >= jobProperties.maxAttempts()) {
                    document.markFailed();
                } else if (job.type().startsWith(JOB_TYPE_INDEX + ":")) {
                    if (document.status() == DocumentStatus.INDEXING) {
                        document.markChunked();
                    }
                } else if (job.type().startsWith(JOB_TYPE_PARSE + ":")) {
                    document.markParsing();
                } else {
                    document.markParsing();
                }
                documents.save(document);
            } catch (RuntimeException ignored) {
                // 保留原始错误供队列重试
            }
        }
        jobs.retry(job.id(), errorMessage(ex), now);
    }

    private static boolean isIngestJob(String type) {
        return type.startsWith(JOB_TYPE + ":")
                || type.startsWith(JOB_TYPE_PARSE + ":")
                || type.startsWith(JOB_TYPE_INDEX + ":");
    }

    private static boolean isLegacyGeneral(KnowledgeDocument document) {
        return "GENERAL".equalsIgnoreCase(document.chunkStrategy()) && document.chunkStrategyVersion() == 1;
    }

    static Long parseDocumentId(String jobType) {
        int separator = jobType.lastIndexOf(':');
        if (separator < 0 || separator == jobType.length() - 1) {
            throw new IllegalArgumentException("Invalid ingest job type");
        }
        return Long.parseLong(jobType.substring(separator + 1));
    }

    private static String errorMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
