package com.gymmind.agent.application;

import com.gymmind.agent.domain.model.TenantAgent;
import com.gymmind.agent.domain.model.AgentStatus;
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
import static org.mockito.Mockito.when;

class AgentManagementServiceTest {

    @Test
    void gymAdminCannotCreateAnotherAgent() {
        var service = new DefaultAgentManagementService(
                mock(TenantAgentRepository.class), mock(TenantQuotaService.class), mock(AuditRecorder.class));

        assertThatThrownBy(() -> service.create(admin(7L), new CreateAgentCommand(
                "私教助手", "desc", "你是教练助手", true, List.of("knowledge_search"))))
                .isInstanceOf(BusinessException.class);
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
    void platformAdminCannotUseTenantAgentListWithoutTenantContext() {
        var service = new DefaultAgentManagementService(mock(TenantAgentRepository.class),
                mock(TenantQuotaService.class), mock(AuditRecorder.class));
        var platformAdmin = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN),
                Set.of("platform:agent:read"), 0, "platform");
        assertThatThrownBy(() -> service.list(platformAdmin)).isInstanceOf(BusinessException.class);
    }

    @Test
    void cannotAccessOtherTenantAgent() {
        var agents = mock(TenantAgentRepository.class);
        when(agents.findByTenantIdAndId(7L, 99L)).thenReturn(Optional.empty());
        var service = new DefaultAgentManagementService(agents, mock(TenantQuotaService.class), mock(AuditRecorder.class));
        assertThatThrownBy(() -> service.find(admin(7L), 99L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void gymAdminCanUpdateOnlyThePromptForOwnAgent() throws Exception {
        var agents = mock(TenantAgentRepository.class);
        var agent = TenantAgent.create(7L, "助手", "desc", "旧提示词", true, "knowledge_search", 1L);
        when(agents.findByTenantIdAndId(7L, 3L)).thenReturn(Optional.of(agent));
        when(agents.findAllByTenantIdAndStatus(7L, AgentStatus.ACTIVE)).thenReturn(List.of(agent));
        when(agents.save(any(TenantAgent.class))).thenAnswer(inv -> inv.getArgument(0));
        var service = new DefaultAgentManagementService(agents, mock(TenantQuotaService.class), mock(AuditRecorder.class));

        AgentView view = service.updatePrompt(admin(7L), 3L, "  新提示词  ");

        assertThat(view.systemPrompt()).isEqualTo("新提示词");
        assertThat(view.name()).isEqualTo("助手");
        assertThat(view.description()).isEqualTo("desc");
        assertThat(view.enabledTools()).containsExactly("knowledge_search");
    }

    @Test
    void memberCannotUpdateAnAgentPrompt() {
        var service = new DefaultAgentManagementService(
                mock(TenantAgentRepository.class), mock(TenantQuotaService.class), mock(AuditRecorder.class));

        assertThatThrownBy(() -> service.updatePrompt(member(7L), 3L, "新提示词"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void platformAdminCannotUpdateATenantAgentPrompt() {
        var service = new DefaultAgentManagementService(
                mock(TenantAgentRepository.class), mock(TenantQuotaService.class), mock(AuditRecorder.class));
        var platformAdmin = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN),
                Set.of("agent:write"), 0, "platform");

        assertThatThrownBy(() -> service.updatePrompt(platformAdmin, 3L, "新提示词"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void promptUpdateRejectsBlankAndOverlongValues() throws Exception {
        var agents = mock(TenantAgentRepository.class);
        var agent = TenantAgent.create(7L, "助手", "", "旧提示词", true, "knowledge_search", 1L);
        when(agents.findByTenantIdAndId(7L, 3L)).thenReturn(Optional.of(agent));
        when(agents.findAllByTenantIdAndStatus(7L, AgentStatus.ACTIVE)).thenReturn(List.of(agent));
        var service = new DefaultAgentManagementService(agents, mock(TenantQuotaService.class), mock(AuditRecorder.class));

        assertThatThrownBy(() -> service.updatePrompt(admin(7L), 3L, "   "))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.updatePrompt(admin(7L), 3L, "x".repeat(4001)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void gymAdminCannotUpdateASecondaryActiveAgentPrompt() throws Exception {
        var agents = mock(TenantAgentRepository.class);
        var first = TenantAgent.create(7L, "默认助手", "", "默认提示词", true, "knowledge_search", 1L);
        var secondary = TenantAgent.create(7L, "其他助手", "", "其他提示词", true, "knowledge_search", 1L);
        when(agents.findAllByTenantIdAndStatus(7L, AgentStatus.ACTIVE)).thenReturn(List.of(first, secondary));
        when(agents.findByTenantIdAndId(7L, 9L)).thenReturn(Optional.of(secondary));
        var service = new DefaultAgentManagementService(agents, mock(TenantQuotaService.class), mock(AuditRecorder.class));

        assertThatThrownBy(() -> service.updatePrompt(admin(7L), 9L, "新提示词"))
                .isInstanceOf(BusinessException.class);
    }

    private static CurrentActor admin(long tenantId) {
        return new CurrentActor(1L, tenantId, Set.of(RoleCode.GYM_ADMIN),
                Set.of("agent:write", "agent:read", "agent:chat"), 0, "t");
    }

    private static CurrentActor member(long tenantId) {
        return new CurrentActor(2L, tenantId, Set.of(RoleCode.MEMBER), Set.of("agent:chat", "agent:read"), 0, "t");
    }
}
