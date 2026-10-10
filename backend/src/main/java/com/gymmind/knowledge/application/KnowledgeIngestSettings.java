package com.gymmind.knowledge.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeIngestSettings {

    private final boolean twoPhase;

    public KnowledgeIngestSettings(
            @Value("${gymmind.knowledge.ingest.two-phase:false}") boolean twoPhase) {
        this.twoPhase = twoPhase;
    }

    /** 测试与 legacy Worker 构造使用 */
    static KnowledgeIngestSettings disabled() {
        return new KnowledgeIngestSettings(false);
    }

    public boolean twoPhase() {
        return twoPhase;
    }
}
