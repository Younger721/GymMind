package com.gymmind.knowledge.application;

import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import com.gymmind.knowledge.infrastructure.search.KnowledgeIndexingService;
import com.gymmind.shared.async.PersistentJobQueue;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class KnowledgeIngestWorker {

    public static final String JOB_TYPE = "KNOWLEDGE_INGEST";
    private static final int MAX_ATTEMPTS = 5;

    private final PersistentJobQueue jobs;
    private final KnowledgeDocumentRepository documents;
    private final KnowledgeObjectStore store;
    private final KnowledgeIndexingService indexing;
    private final Clock clock;

    public KnowledgeIngestWorker(
            PersistentJobQueue jobs,
            KnowledgeDocumentRepository documents,
            KnowledgeObjectStore store,
            KnowledgeIndexingService indexing,
            Clock clock) {
        this.jobs = jobs;
        this.documents = documents;
        this.store = store;
        this.indexing = indexing;
        this.clock = clock;
    }

    public void enqueue(Long tenantId, Long documentId) {
        if (documentId == null) {
            return;
        }
        jobs.enqueue(JOB_TYPE + ":" + documentId, tenantId);
    }

    @Scheduled(fixedDelayString = "${gymmind.knowledge.ingest-delay-ms:5000}")
    @Transactional
    public void processNext() {
        Instant now = clock.instant();
        jobs.recoverExpired(now);
        jobs.claimNext(now, Duration.ofMinutes(5)).ifPresent(job -> {
            if (job.type() == null || !job.type().startsWith(JOB_TYPE)) {
                jobs.complete(job.id());
                return;
            }
            KnowledgeDocument document = null;
            try {
                Long documentId = parseDocumentId(job.type());
                document = job.tenantId() == null
                        ? documents.findForUpdateById(documentId).orElse(null)
                        : documents.findForUpdateByTenantIdAndId(job.tenantId(), documentId).orElse(null);
                if (document == null || document.status() == com.gymmind.knowledge.domain.model.DocumentStatus.DELETED) {
                    jobs.complete(job.id());
                    return;
                }
                document.markParsing();
                documents.save(document);
                byte[] bytes = store.read(document.objectKey());
                if (bytes == null) {
                    throw new IllegalStateException("knowledge object is unavailable");
                }
                // Backfill strategy metadata on legacy documents while rebuilding.
                document.updateChunkStrategy(document.chunkStrategy(), document.chunkStrategyVersion());
                boolean published = isLegacyGeneral(document)
                        ? indexing.publish(document.tenantId(), document.id(), document.ownerUserId(),
                        document.fileName(), document.contentType(), bytes)
                        : indexing.publish(document.tenantId(), document.id(), document.ownerUserId(),
                        document.fileName(), document.contentType(), bytes,
                        document.chunkStrategy(), document.chunkStrategyVersion());
                if (!published) {
                    throw new IllegalStateException("knowledge indexing is unavailable");
                }
                KnowledgeDocument current = document.tenantId() == null
                        ? documents.findForUpdateById(document.id()).orElse(null)
                        : documents.findForUpdateByTenantIdAndId(document.tenantId(), document.id()).orElse(null);
                if (current == null || current.status() == com.gymmind.knowledge.domain.model.DocumentStatus.DELETED) {
                    // A delete may have completed while the external index was publishing.
                    // Remove the just-published generation and never resurrect the tombstone.
                    indexing.deleteDocument(document.tenantId(), document.id());
                    jobs.complete(job.id());
                    return;
                }
                current.markReady();
                documents.save(current);
                jobs.complete(job.id());
            } catch (RuntimeException ex) {
                if (document != null && document.status() != com.gymmind.knowledge.domain.model.DocumentStatus.DELETED) {
                    try {
                        if (job.attempts() + 1 >= MAX_ATTEMPTS) {
                            document.markFailed();
                        } else {
                            // Keep the document visibly in progress while the queue schedules a retry.
                            document.markParsing();
                        }
                        documents.save(document);
                    } catch (RuntimeException ignored) {
                        // Preserve the original processing error and let the queue retry it.
                    }
                }
                jobs.retry(job.id(), errorMessage(ex), now);
            }
        });
    }

    private static boolean isLegacyGeneral(KnowledgeDocument document) {
        return "GENERAL".equalsIgnoreCase(document.chunkStrategy()) && document.chunkStrategyVersion() == 1;
    }

    private static Long parseDocumentId(String jobType) {
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
