package com.gymmind.audit.application;

import com.gymmind.ai.application.AiUsageRecord;
import com.gymmind.ai.application.AiUsageRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AiUsageQueryServiceFilteringTest {
    @Test
    void filtersByDateAndModelAndSummarizesAllMatchingRows() {
        AiUsageRecord first = record(7L, "qwen-plus", 10, 4, Instant.parse("2026-10-01T01:00:00Z"));
        AiUsageRecord second = record(7L, "qwen-plus", 5, 2, Instant.parse("2026-10-02T01:00:00Z"));
        AiUsageRecord otherModel = record(7L, "other", 100, 100, Instant.parse("2026-10-02T01:00:00Z"));
        var service = new DefaultAiUsageQueryService(new InMemoryRepository(List.of(first, second, otherModel)));

        AiUsagePage page = service.search(gymAdmin(7L), new AiUsageFilter(
                Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-03T00:00:00Z"),
                null, "qwen-plus", null, 0, 20));

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.summary().inputTokens()).isEqualTo(15);
        assertThat(page.summary().outputTokens()).isEqualTo(6);
    }

    private static AiUsageRecord record(long tenant, String model, int input, int output, Instant createdAt) {
        return new AiUsageRecord(tenant, 2L, model, input, output, 20, "req", "OK", null,
                "dashscope", "chat", input + output, "PROVIDER", new BigDecimal("0.01"), "CNY", createdAt);
    }

    private static CurrentActor gymAdmin(long tenant) {
        return new CurrentActor(2L, tenant, Set.of(RoleCode.GYM_ADMIN), Set.of(), 0, "token");
    }

    private record InMemoryRepository(List<AiUsageRecord> rows) implements AiUsageRepository {
        @Override public AiUsageRecord save(AiUsageRecord record) { return record; }
        @Override public List<AiUsageRecord> findAllByTenantId(Long tenantId) {
            return rows.stream().filter(row -> tenantId.equals(row.tenantId())).toList();
        }
    }
}
