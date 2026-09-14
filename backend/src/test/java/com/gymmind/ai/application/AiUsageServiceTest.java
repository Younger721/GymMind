package com.gymmind.ai.application;

import com.gymmind.shared.security.CurrentActor;
import com.gymmind.iam.domain.model.RoleCode;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class AiUsageServiceTest {
    @Test void recordsTenantScopedUsageWithoutPromptContent() {
        var repo = new InMemoryAiUsageRepository();
        var service = new DefaultAiUsageService(repo);
        service.record(actor(7L), new AiUsageCommand("qwen-plus", 10, 20, 55, "req-1", "OK", "secret prompt"));
        assertThat(repo.items).singleElement().satisfies(x -> {
            assertThat(x.tenantId()).isEqualTo(7L);
            assertThat(x.prompt()).isNull();
            assertThat(x.inputTokens()).isEqualTo(10);
        });
    }
    private CurrentActor actor(Long tenant){return new CurrentActor(1L,tenant,Set.of(RoleCode.MEMBER),Set.of("ai:chat"),0,"t");}
    static class InMemoryAiUsageRepository implements AiUsageRepository {
        java.util.List<AiUsageRecord> items=new java.util.ArrayList<>();
        public AiUsageRecord save(AiUsageRecord r){items.add(r);return r;}
        public java.util.List<AiUsageRecord> findAllByTenantId(Long tenantId) {
            return items.stream().filter(item -> tenantId.equals(item.tenantId())).toList();
        }
    }
}
