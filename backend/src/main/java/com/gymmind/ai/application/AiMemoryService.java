package com.gymmind.ai.application;

import com.gymmind.shared.security.CurrentActor;

import java.util.List;

public interface AiMemoryService {
    void write(CurrentActor actor, String key, String value);

    List<AiMemoryEntry> list(CurrentActor actor);

    void delete(CurrentActor actor, String key);
}
