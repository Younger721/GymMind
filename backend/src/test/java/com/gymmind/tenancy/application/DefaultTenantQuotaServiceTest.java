package com.gymmind.tenancy.application;

import com.gymmind.agent.domain.repository.TenantAgentRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.knowledge.application.KnowledgeDocumentRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.domain.model.TenantQuota;
import com.gymmind.tenancy.domain.repository.TenantQuotaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultTenantQuotaServiceTest {

    @Test
    void defaultsToOneAgentPerTenant() {
        assertThat(TenantQuota.defaults(7L).getMaxAgents()).isEqualTo(1);
    }

    @Test
    void reportsOneAgentLimitForLegacyQuotaAndPreservesUsage() {
        TenantQuota quota = TenantQuota.defaults(7L);
        quota.update(5, 100, 10, 20, true, true);

        TenantQuotaView view = TenantQuotaView.of(quota, 3, 11, 12, 13);

        assertThat(view.maxAgents()).isEqualTo(1);
        assertThat(view.usedAgents()).isEqualTo(3);
    }

    @Test
    void blocksAgentCreationWhenOneAgentExistsEvenIfLegacyQuotaIsHigher() {
        TenantQuota quota = TenantQuota.defaults(7L);
        quota.update(5, 100, 10, 20, true, true);
        Fixture fixture = fixture(quota, mock(JdbcTemplate.class));
        when(fixture.agents().countByTenantId(7L)).thenReturn(1L);

        assertThatThrownBy(() -> fixture.service().ensureAgentCreationAllowed(7L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsPlatformQuotaUpdateWhenAgentLimitIsNotOneWithoutSaving() {
        TenantQuota quota = TenantQuota.defaults(7L);
        quota.update(5, 100, 1_000, 100, true, true);
        Fixture fixture = fixture(quota, mock(JdbcTemplate.class));
        when(fixture.quotas().save(any(TenantQuota.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(fixture.jdbc().queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(0L);
        when(fixture.documents().findByTenantId(7L)).thenReturn(List.of());

        assertThatThrownBy(() -> fixture.service().updateForPlatform(platformAdmin(), 7L,
                new UpdateTenantQuotaCommand(2, 120, 12_000, 8, true, true)))
                .isInstanceOf(BusinessException.class);

        verify(fixture.quotas(), never()).save(any(TenantQuota.class));
        assertThat(quota.getMaxAgents()).isEqualTo(5);
    }

    @Test
    void updatesOtherQuotasWhenPlatformKeepsAgentLimitAtOne() {
        TenantQuota quota = TenantQuota.defaults(7L);
        Fixture fixture = fixture(quota, mock(JdbcTemplate.class));
        when(fixture.quotas().save(any(TenantQuota.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(fixture.jdbc().queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(0L);
        when(fixture.documents().findByTenantId(7L)).thenReturn(List.of());

        TenantQuotaView view = fixture.service().updateForPlatform(platformAdmin(), 7L,
                new UpdateTenantQuotaCommand(1, 120, 12_000, 8, true, true));

        assertThat(view.maxAgents()).isEqualTo(1);
        assertThat(view.maxAiCallsPerMonth()).isEqualTo(120);
        assertThat(view.maxAiTokensPerMonth()).isEqualTo(12_000);
        assertThat(view.maxKnowledgeDocuments()).isEqualTo(8);
    }

    @Test
    void rejectsAiChatWhenMonthlyTokenQuotaIsExhausted() {
        TenantQuota quota = TenantQuota.defaults(7L);
        quota.update(5, 100, 10, 20, true, true);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(0L, 10L);

        assertThatThrownBy(() -> fixture(quota, jdbc).service().ensureAiChatAllowed(7L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void allowsAiChatWhileMonthlyTokenQuotaHasCapacity() {
        TenantQuota quota = TenantQuota.defaults(7L);
        quota.update(5, 100, 10, 20, true, true);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(0L, 9L);

        fixture(quota, jdbc).service().ensureAiChatAllowed(7L);
    }

    private static Fixture fixture(TenantQuota quota, JdbcTemplate jdbc) {
        TenantQuotaRepository quotas = mock(TenantQuotaRepository.class);
        TenantAgentRepository agents = mock(TenantAgentRepository.class);
        KnowledgeDocumentRepository documents = mock(KnowledgeDocumentRepository.class);
        when(quotas.findByTenantId(7L)).thenReturn(Optional.of(quota));
        DefaultTenantQuotaService service = new DefaultTenantQuotaService(quotas, agents, documents, jdbc);
        return new Fixture(service, quotas, agents, documents, jdbc);
    }

    private static CurrentActor platformAdmin() {
        return new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN),
                Set.of("platform:quota:write"), 0L, "platform-token");
    }

    private record Fixture(
            DefaultTenantQuotaService service,
            TenantQuotaRepository quotas,
            TenantAgentRepository agents,
            KnowledgeDocumentRepository documents,
            JdbcTemplate jdbc) {
    }
}
