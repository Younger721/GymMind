package com.gymmind.tenancy.application;

import com.gymmind.agent.domain.repository.TenantAgentRepository;
import com.gymmind.knowledge.application.KnowledgeDocumentRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.tenancy.domain.model.TenantQuota;
import com.gymmind.tenancy.domain.repository.TenantQuotaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultTenantQuotaServiceTest {

    @Test
    void rejectsAiChatWhenMonthlyTokenQuotaIsExhausted() {
        TenantQuota quota = TenantQuota.defaults(7L);
        quota.update(5, 100, 10, 20, true, true);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(0L, 10L);
        DefaultTenantQuotaService service = service(quota, jdbc);

        assertThatThrownBy(() -> service.ensureAiChatAllowed(7L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void allowsAiChatWhileMonthlyTokenQuotaHasCapacity() {
        TenantQuota quota = TenantQuota.defaults(7L);
        quota.update(5, 100, 10, 20, true, true);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(0L, 9L);
        DefaultTenantQuotaService service = service(quota, jdbc);

        service.ensureAiChatAllowed(7L);
    }

    private static DefaultTenantQuotaService service(TenantQuota quota, JdbcTemplate jdbc) {
        TenantQuotaRepository quotas = mock(TenantQuotaRepository.class);
        when(quotas.findByTenantId(7L)).thenReturn(Optional.of(quota));
        return new DefaultTenantQuotaService(quotas, mock(TenantAgentRepository.class),
                mock(KnowledgeDocumentRepository.class), jdbc);
    }
}
