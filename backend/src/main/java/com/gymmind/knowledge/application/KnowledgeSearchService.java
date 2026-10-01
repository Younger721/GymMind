package com.gymmind.knowledge.application;

import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.harness.AiProgressEvent;
import com.gymmind.shared.security.CurrentActor;

import java.util.List;
import java.util.function.Consumer;

public interface KnowledgeSearchService {
    List<ContextSegment> search(CurrentActor actor, String query);

    default List<ContextSegment> search(CurrentActor actor, String query,
                                        Consumer<AiProgressEvent> progress) {
        return search(actor, query);
    }
}
