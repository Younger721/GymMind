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
    public void write(CurrentActor actor, AiMemoryType type, String key, String value) {
        MemoryScope scope = requireScope(actor);
        String compositeKey = compositeKey(type, key);
        memories.computeIfAbsent(scope, ignored -> new ConcurrentHashMap<>()).put(compositeKey, requireText(value));
    }

    @Override
    public List<AiMemoryEntry> list(CurrentActor actor) {
        MemoryScope scope = requireScope(actor);
        return memories.getOrDefault(scope, Map.of()).entrySet().stream()
                .map(entry -> toEntry(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(AiMemoryEntry::type).thenComparing(AiMemoryEntry::key))
                .toList();
    }

    @Override
    public void delete(CurrentActor actor, AiMemoryType type, String key) {
        MemoryScope scope = requireScope(actor);
        Map<String, String> scopedMemories = memories.get(scope);
        if (scopedMemories != null) {
            scopedMemories.remove(compositeKey(type, key));
        }
    }

    private static String compositeKey(AiMemoryType type, String key) {
        if (type == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        return type.name() + ':' + requireText(key);
    }

    private static AiMemoryEntry toEntry(String compositeKey, String value) {
        int separator = compositeKey.indexOf(':');
        return new AiMemoryEntry(AiMemoryType.valueOf(compositeKey.substring(0, separator)),
                compositeKey.substring(separator + 1), value);
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
