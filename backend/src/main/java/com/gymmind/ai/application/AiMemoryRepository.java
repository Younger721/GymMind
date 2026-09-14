package com.gymmind.ai.application;
import java.util.List;
public interface AiMemoryRepository {
    void save(Long tenantId, Long userId, AiMemoryType type, String key, String value);
    List<AiMemoryEntry> findAll(Long tenantId, Long userId);
    void delete(Long tenantId, Long userId, AiMemoryType type, String key);
}
