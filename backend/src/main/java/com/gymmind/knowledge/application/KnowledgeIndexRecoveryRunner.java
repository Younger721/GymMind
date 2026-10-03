package com.gymmind.knowledge.application;

import com.gymmind.knowledge.domain.model.DocumentStatus;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Rebuilds volatile search indexes after a development restart. */
@Component
public class KnowledgeIndexRecoveryRunner implements ApplicationRunner {
    private final KnowledgeDocumentRepository documents;
    private final KnowledgeIngestWorker worker;

    public KnowledgeIndexRecoveryRunner(KnowledgeDocumentRepository documents, KnowledgeIngestWorker worker) {
        this.documents = documents;
        this.worker = worker;
    }

    @Override
    public void run(ApplicationArguments args) {
        documents.findAllDocuments().stream()
                .filter(KnowledgeIndexRecoveryRunner::shouldRecover)
                .forEach(document -> worker.enqueue(document.tenantId(), document.id()));
    }

    private static boolean shouldRecover(KnowledgeDocument document) {
        return document.status() == DocumentStatus.UPLOADING
                || document.status() == DocumentStatus.PARSING;
    }
}
