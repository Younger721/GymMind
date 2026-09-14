package com.gymmind.ai.infrastructure;

import com.gymmind.ai.application.AiUsageRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JpaAiUsageRepositoryTest {

    @Test
    void savesUsageWithoutPersistingPromptContent() {
        SpringDataAiUsageRepository delegate = mock(SpringDataAiUsageRepository.class);
        when(delegate.save(any(AiUsageEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        JpaAiUsageRepository repository = new JpaAiUsageRepository(delegate);

        AiUsageRecord saved = repository.save(new AiUsageRecord(
                7L, 19L, "qwen-plus", 120, 45, 380L, "req-42", "OK", "private prompt"));

        assertThat(saved).isEqualTo(new AiUsageRecord(
                7L, 19L, "qwen-plus", 120, 45, 380L, "req-42", "OK", null));
    }

    @Test
    void readsOnlyRecordsReturnedForRequestedTenant() {
        SpringDataAiUsageRepository delegate = mock(SpringDataAiUsageRepository.class);
        when(delegate.findAllByTenantIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(
                AiUsageEntity.from(new AiUsageRecord(7L, 19L, "qwen-plus", 10, 5, 80L, "req-1", "OK", null))));
        JpaAiUsageRepository repository = new JpaAiUsageRepository(delegate);

        List<AiUsageRecord> records = repository.findAllByTenantId(7L);

        assertThat(records).containsExactly(
                new AiUsageRecord(7L, 19L, "qwen-plus", 10, 5, 80L, "req-1", "OK", null));
    }
}
