package com.gymmind.agent.application;

import com.gymmind.agent.domain.model.TenantAgent;
import com.gymmind.agent.domain.repository.TenantAgentRepository;
import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.application.TenantQuotaService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentManagementServiceTest {

    @Test
    void gymAdminCanCreateAgent() {
        var agents = mock(TenantAgentRepository.class);
        var quotas = mock(TenantQuotaService.class);
        when(agents.save(any(TenantAgent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(agents.countByTenantId(7L)).thenReturn(0L);

        var service = new DefaultAgentManagementService(agents, quotas, mock(AuditRecorder.class));
        var view = service.create(admin(7L), new CreateAgentCommand(
                "私教助手", "desc", "你是教练助手", true, List.of("knowledge_search")));

        assertThat(view.name()).isEqualTo("私教助手");
        verify(quotas).ensureAgentCreationAllowed(7L);
    }

    @Test
    void memberCannotCreateAgent() {
        var service = new DefaultAgentManagementService(
                mock(TenantAgentRepository.class),
                mock(TenantQuotaService.class),
                mock(AuditRecorder.class));
        assertThatThrownBy(() -> service.create(member(7L), new CreateAgentCommand(
                "x", "", "prompt", true, List.of("knowledge_search"))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void memberOnlySeesActiveAgents() {
        var agents = mock(TenantAgentRepository.class);
        var active = TenantAgent.create(7L, "A", "", "prompt", true, "knowledge_search", 1L);
        when(agents.findAllByTenantIdAndStatus(7L, com.gymmind.agent.domain.model.AgentStatus.ACTIVE))
                .thenReturn(List.of(active));

        var service = new DefaultAgentManagementService(agents, mock(TenantQuotaService.class), mock(AuditRecorder.class));
        assertThat(service.list(member(7L))).hasSize(1);
    }

    @Test
    void platformAdminWithWhitelistCanListAllTenantAgents() {
        var agents = mock(TenantAgentRepository.class);
        var agentA = TenantAgent.create(7L, "A", "", "prompt", true, "knowledge_search", 1L);
        var agentB = TenantAgent.create(8L, "B", "", "prompt", true, "knowledge_search", 2L);
        when(agents.findAllAccessible()).thenReturn(List.of(agentA, agentB));

        var service = new DefaultAgentManagementService(agents, mock(TenantQuotaService.class), mock(AuditRecorder.class));
        var platformAdmin = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN),
                Set.of("platform:agent:read"), 0, "platform");
        assertThat(service.list(platformAdmin)).hasSize(2);
    }

    @Test
    void cannotAccessOtherTenantAgent() {
        var agents = mock(TenantAgentRepository.class);
        when(agents.findByTenantIdAndId(7L, 99L)).thenReturn(Optional.empty());
        var service = new DefaultAgentManagementService(agents, mock(TenantQuotaService.class), mock(AuditRecorder.class));
        assertThatThrownBy(() -> service.find(admin(7L), 99L)).isInstanceOf(BusinessException.class);
    }

    private static CurrentActor admin(long tenantId) {
        return new CurrentActor(1L, tenantId, Set.of(RoleCode.GYM_ADMIN),
                Set.of("agent:write", "agent:read", "agent:chat"), 0, "t");
    }

    private static CurrentActor member(long tenantId) {
        return new CurrentActor(2L, tenantId, Set.of(RoleCode.MEMBER), Set.of("agent:chat", "agent:read"), 0, "t");
    }
}
