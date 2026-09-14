package com.gymmind.ai.application;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AiMemoryServiceTest {
    @Test
    void memorySurvivesServiceRecreationAndIsIsolatedByTenantAndUser() {
        var repository = new InMemoryAiMemoryRepository();
        var writer = new DefaultAiMemoryService(repository);
        var actor = actor(7L, 1L);
        writer.write(actor, AiMemoryType.GOAL, "weight", "lose weight");

        var reader = new DefaultAiMemoryService(repository);

        assertThat(reader.list(actor)).containsExactly(new AiMemoryEntry(AiMemoryType.GOAL, "weight", "lose weight"));
        assertThat(reader.list(actor(8L, 1L))).isEmpty();
        assertThat(reader.list(actor(7L, 2L))).isEmpty();
        reader.delete(actor, AiMemoryType.GOAL, "weight");
        assertThat(reader.list(actor)).isEmpty();
    }

    @Test
    void writingSameScopedKeyUpdatesItsValue() {
        var repository = new InMemoryAiMemoryRepository();
        var service = new DefaultAiMemoryService(repository);
        var actor = actor(7L, 1L);
        service.write(actor, AiMemoryType.PREFERENCE, "training", "morning");
        service.write(actor, AiMemoryType.PREFERENCE, "training", "evening");
        assertThat(service.list(actor)).containsExactly(
                new AiMemoryEntry(AiMemoryType.PREFERENCE, "training", "evening"));
    }

    private CurrentActor actor(long tenantId, long userId) {
        return new CurrentActor(userId, tenantId, Set.of(RoleCode.MEMBER), Set.of("ai:memory"), 0, "t");
    }

    static class InMemoryAiMemoryRepository implements AiMemoryRepository {
        private final List<ScopedEntry> entries = new ArrayList<>();
        public void save(Long tenantId, Long userId, AiMemoryType type, String key, String value) {
            delete(tenantId, userId, type, key);
            entries.add(new ScopedEntry(tenantId, userId, new AiMemoryEntry(type, key, value)));
        }
        public List<AiMemoryEntry> findAll(Long tenantId, Long userId) {
            return entries.stream().filter(e -> e.tenantId.equals(tenantId) && e.userId.equals(userId))
                    .map(ScopedEntry::entry).toList();
        }
        public void delete(Long tenantId, Long userId, AiMemoryType type, String key) {
            entries.removeIf(e -> e.tenantId.equals(tenantId) && e.userId.equals(userId)
                    && e.entry.type() == type && e.entry.key().equals(key));
        }
        private record ScopedEntry(Long tenantId, Long userId, AiMemoryEntry entry) {}
    }
}
