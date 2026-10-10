package com.gymmind.knowledge.application;

import com.gymmind.knowledge.domain.model.DocumentStatus;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 启动时恢复中断中的入库；可选是否重灌已发布 READY 文档（默认关闭）。 */
@Component
public class KnowledgeIndexRecoveryRunner implements ApplicationRunner {
    private final KnowledgeDocumentRepository documents;
    private final KnowledgeIngestWorker worker;
    private final KnowledgeIngestSettings ingestSettings;
    private final boolean recoverIncompleteIngest;
    private final boolean recoverPublishedReady;

    @org.springframework.beans.factory.annotation.Autowired
    public KnowledgeIndexRecoveryRunner(KnowledgeDocumentRepository documents, KnowledgeIngestWorker worker,
                                        KnowledgeIngestSettings ingestSettings,
                                        @Value("${gymmind.search.recover-incomplete-ingest:true}") boolean recoverIncompleteIngest,
                                        @Value("${gymmind.search.recover-published:false}") boolean recoverPublished,
                                        @Value("${gymmind.search.recover-index-on-start:false}") boolean recoverIndexOnStart) {
        this(documents, worker, ingestSettings, recoverIncompleteIngest,
                recoverPublished || recoverIndexOnStart);
    }

    KnowledgeIndexRecoveryRunner(KnowledgeDocumentRepository documents, KnowledgeIngestWorker worker,
                                 KnowledgeIngestSettings ingestSettings,
                                 boolean recoverIncompleteIngest, boolean recoverPublishedReady) {
        this.documents = documents;
        this.worker = worker;
        this.ingestSettings = ingestSettings;
        this.recoverIncompleteIngest = recoverIncompleteIngest;
        this.recoverPublishedReady = recoverPublishedReady;
    }

    @Override
    public void run(ApplicationArguments args) {
        documents.findAllDocuments().stream()
                .filter(this::shouldRecover)
                .forEach(this::recover);
    }

    private void recover(com.gymmind.knowledge.domain.model.KnowledgeDocument document) {
        if (ingestSettings.twoPhase()
                && (document.status() == DocumentStatus.CHUNKED || document.status() == DocumentStatus.INDEXING)) {
            worker.enqueueIndex(document.tenantId(), document.id());
        } else {
            worker.enqueue(document.tenantId(), document.id());
        }
    }

    private boolean shouldRecover(KnowledgeDocument document) {
        if (document.status() == DocumentStatus.UPLOADING
                || document.status() == DocumentStatus.PARSING
                || document.status() == DocumentStatus.CHUNKED
                || document.status() == DocumentStatus.INDEXING) {
            return recoverIncompleteIngest;
        }
        return recoverPublishedReady
                && document.status() == DocumentStatus.READY
                && document.published();
    }
}
