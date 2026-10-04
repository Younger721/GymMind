package com.gymmind.ai.harness;

import com.gymmind.agent.domain.model.AgentStatus;
import com.gymmind.agent.domain.model.TenantAgent;
import com.gymmind.agent.domain.repository.TenantAgentRepository;
import com.gymmind.ai.application.ChatModelGateway;
import com.gymmind.ai.harness.application.DefaultAiHarnessService;
import com.gymmind.ai.harness.config.AiHarnessProperties;
import com.gymmind.ai.harness.domain.PlatformHarnessConfigRepository;
import com.gymmind.ai.harness.domain.TenantHarnessConfig;
import com.gymmind.ai.harness.domain.TenantHarnessConfigRepository;
import com.gymmind.ai.harness.plugins.BasicChatHarnessPlugin;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantAgentPromptIntegrationTest {

    @Test
    void harnessUsesTheCurrentTenantsActiveAgentPrompt() throws Exception {
        TenantHarnessConfigRepository tenants = mock(TenantHarnessConfigRepository.class);
        when(tenants.findByTenantId(7L)).thenReturn(Optional.of(
                TenantHarnessConfig.defaults(7L, "basic_chat", "Harness fallback")));
        TenantAgentRepository agents = mock(TenantAgentRepository.class);
        TenantAgent agent = TenantAgent.create(7L, "门店助手", "", "门店专属提示词", true,
                "knowledge_search", 1L);
        when(agents.findAllByTenantIdAndStatus(7L, AgentStatus.ACTIVE))
                .thenReturn(List.of(agent));
        ChatModelGateway model = mock(ChatModelGateway.class);
        when(model.stream(anyString(), anyList(), any())).thenReturn("answer");

        DefaultAiHarnessService service = newService(agents, tenants, model);
        service.chat(new CurrentActor(1L, 7L, Set.of(RoleCode.MEMBER), Set.of("ai:chat"), 0, "t"),
                "session", "问题", List.of("basic_chat"), List.of(), ignored -> { }, ignored -> { });

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(model).stream(prompt.capture(), anyList(), any());
        assertThat(prompt.getValue()).contains("门店专属提示词");
        assertThat(prompt.getValue()).doesNotContain("Harness fallback");
    }

    @Test
    void harnessFallsBackWhenTheTenantHasNoActiveAgent() throws Exception {
        TenantHarnessConfigRepository tenants = mock(TenantHarnessConfigRepository.class);
        when(tenants.findByTenantId(7L)).thenReturn(Optional.of(
                TenantHarnessConfig.defaults(7L, "basic_chat", "Harness fallback")));
        TenantAgentRepository agents = mock(TenantAgentRepository.class);
        when(agents.findAllByTenantIdAndStatus(7L, AgentStatus.ACTIVE))
                .thenReturn(List.of());
        ChatModelGateway model = mock(ChatModelGateway.class);
        when(model.stream(anyString(), anyList(), any())).thenReturn("answer");

        DefaultAiHarnessService service = newService(agents, tenants, model);
        service.chat(new CurrentActor(1L, 7L, Set.of(RoleCode.MEMBER), Set.of("ai:chat"), 0, "t"),
                "session", "问题", List.of("basic_chat"), List.of(), ignored -> { }, ignored -> { });

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(model).stream(prompt.capture(), anyList(), any());
        assertThat(prompt.getValue()).contains("Harness fallback");
    }

    private static DefaultAiHarnessService newService(TenantAgentRepository agents,
                                                        TenantHarnessConfigRepository tenants,
                                                        ChatModelGateway model) throws Exception {
        Constructor<DefaultAiHarnessService> constructor = DefaultAiHarnessService.class.getConstructor(
                AiHarnessPluginRegistry.class,
                TenantHarnessConfigRepository.class,
                PlatformHarnessConfigRepository.class,
                AiHarnessProperties.class,
                ChatModelGateway.class,
                com.gymmind.ai.application.DefaultAiUsageService.class,
                com.gymmind.tenancy.application.TenantQuotaService.class,
                TenantAgentRepository.class);
        return constructor.newInstance(
                new AiHarnessPluginRegistry(List.of(new BasicChatHarnessPlugin())),
                tenants,
                mock(PlatformHarnessConfigRepository.class),
                new AiHarnessProperties(),
                model,
                null,
                null,
                agents);
    }
}
