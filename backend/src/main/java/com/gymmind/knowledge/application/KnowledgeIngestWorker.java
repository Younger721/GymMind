package com.gymmind.knowledge.application;

import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import com.gymmind.knowledge.infrastructure.search.KnowledgeIndexingService;
import com.gymmind.shared.async.PersistentJobQueue;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class KnowledgeIngestWorker {

    public static final String JOB_TYPE = "KNOWLEDGE_INGEST";

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
        if (tenantId == null || documentId == null) {
            return;
        }
        jobs.enqueue(JOB_TYPE + ":" + documentId, tenantId);
    }

    @Scheduled(fixedDelayString = "${gymmind.knowledge.ingest-delay-ms:5000}")
    public void processNext() {
        jobs.recoverExpired(clock.instant());
        jobs.claimNext(clock.instant(), Duration.ofMinutes(5)).ifPresent(job -> {
            if (job.type() == null || !job.type().startsWith(JOB_TYPE)) {
                jobs.complete(job.id());
                return;
            }
            try {
                Long documentId = parseDocumentId(job.type());
                KnowledgeDocument document = documents.findByTenantIdAndId(job.tenantId(), documentId)
                        .orElse(null);
                if (document == null) {
                    jobs.complete(job.id());
                    return;
                }
                document.markParsing();
                documents.save(document);
                byte[] bytes = store.read(document.objectKey());
                if (bytes != null && indexing.publish(document.tenantId(), document.id(),
                        document.ownerUserId(), document.fileName(), document.contentType(), bytes)) {
                    document.markReady();
                }
                documents.save(document);
                jobs.complete(job.id());
            } catch (RuntimeException ex) {
                jobs.retry(job.id(), ex.getMessage(), Instant.now());
            }
        });
    }

    private static Long parseDocumentId(String jobType) {
        int separator = jobType.lastIndexOf(':');
        if (separator < 0 || separator == jobType.length() - 1) {
            throw new IllegalArgumentException("Invalid ingest job type");
        }
        return Long.parseLong(jobType.substring(separator + 1));
    }
}
