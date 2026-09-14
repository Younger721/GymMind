package com.gymmind.ai.application;

import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DefaultAiMemoryService implements AiMemoryService {
    private final Map<MemoryScope, Map<String, String>> memories = new ConcurrentHashMap<>();

    @Override
    public void write(CurrentActor actor, String key, String value) {
        MemoryScope scope = requireScope(actor);
        String normalizedKey = requireText(key);
        memories.computeIfAbsent(scope, ignored -> new ConcurrentHashMap<>())
                .put(normalizedKey, requireText(value));
    }

    @Override
    public List<AiMemoryEntry> list(CurrentActor actor) {
        MemoryScope scope = requireScope(actor);
        return memories.getOrDefault(scope, Map.of()).entrySet().stream()
                .map(entry -> new AiMemoryEntry(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(AiMemoryEntry::key))
                .toList();
    }

    @Override
    public void delete(CurrentActor actor, String key) {
        MemoryScope scope = requireScope(actor);
        Map<String, String> scopedMemories = memories.get(scope);
        if (scopedMemories != null) {
            scopedMemories.remove(requireText(key));
        }
    }

    private static MemoryScope requireScope(CurrentActor actor) {
        if (actor == null || actor.tenantId() == null || !actor.hasPermission("ai:memory")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return new MemoryScope(actor.tenantId(), actor.userId());
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return value.trim();
    }

    private record MemoryScope(Long tenantId, Long userId) {
    }
}
