package com.gymmind.knowledge.application;
import com.gymmind.knowledge.domain.model.*;
import com.gymmind.shared.error.*;
import com.gymmind.shared.security.CurrentActor;
import java.util.List;
public interface KnowledgeDocumentUseCase {
    KnowledgeDocumentView upload(CurrentActor actor, UploadKnowledgeDocumentCommand command);

    KnowledgeDocumentView find(CurrentActor actor, Long id);

    KnowledgeDocumentView update(CurrentActor actor, Long id, UpdateKnowledgeDocumentCommand command);

    KnowledgeDocumentView reindex(CurrentActor actor, Long id);

    KnowledgeDocumentView delete(CurrentActor actor, Long id);

    boolean canRead(CurrentActor actor, Long id);

    List<KnowledgeDocumentView> list(CurrentActor actor);
}
