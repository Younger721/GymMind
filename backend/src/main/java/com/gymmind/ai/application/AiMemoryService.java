package com.gymmind.ai.application;

import com.gymmind.shared.security.CurrentActor;

import java.util.List;

public interface AiMemoryService {
    void write(CurrentActor actor, AiMemoryType type, String key, String value);

    default void write(CurrentActor actor, String key, String value) {
        write(actor, AiMemoryType.IMPORTANT_FACT, key, value);
    }

    List<AiMemoryEntry> list(CurrentActor actor);

    void delete(CurrentActor actor, AiMemoryType type, String key);

    default void delete(CurrentActor actor, String key) {
        delete(actor, AiMemoryType.IMPORTANT_FACT, key);
    }
}
