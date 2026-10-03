package com.gymmind.knowledge.application;

import com.gymmind.knowledge.domain.model.DocumentStatus;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Rebuilds volatile search indexes after a development restart. */
@Component
public class KnowledgeIndexRecoveryRunner implements ApplicationRunner {
    private final KnowledgeDocumentRepository documents;
    private final KnowledgeIngestWorker worker;
    private final boolean recoverPublished;

    @org.springframework.beans.factory.annotation.Autowired
    public KnowledgeIndexRecoveryRunner(KnowledgeDocumentRepository documents, KnowledgeIngestWorker worker,
                                        @Value("${gymmind.search.recover-published:false}") boolean recoverPublished) {
        this.documents = documents;
        this.worker = worker;
        this.recoverPublished = recoverPublished;
    }

    @Override
    public void run(ApplicationArguments args) {
        documents.findAllDocuments().stream()
                .filter(this::shouldRecover)
                .forEach(document -> worker.enqueue(document.tenantId(), document.id()));
    }

    private boolean shouldRecover(KnowledgeDocument document) {
        return document.status() == DocumentStatus.UPLOADING
                || document.status() == DocumentStatus.PARSING
                || recoverPublished && document.status() == DocumentStatus.READY && document.published()
                ;
    }
}
