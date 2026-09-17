package com.gymmind.agent.application;

import com.gymmind.agent.domain.model.AgentStatus;
import com.gymmind.agent.domain.model.TenantAgent;
import com.gymmind.agent.domain.repository.TenantAgentRepository;
import com.gymmind.ai.application.ChatModelGateway;
import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.knowledge.application.KnowledgeSearchService;
import com.gymmind.mcp.application.McpToolService;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.application.TenantQuotaService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentChatServiceTest {

    @Test
    void usesAgentSystemPromptAndKnowledge() {
        var agents = mock(TenantAgentRepository.class);
        var search = mock(KnowledgeSearchService.class);
        var model = mock(ChatModelGateway.class);
        var agent = TenantAgent.create(7L, "助手", "", "你是门店助手", true, "knowledge_search", 1L);
        when(agents.findByTenantIdAndId(7L, 3L)).thenReturn(Optional.of(agent));
        when(search.search(any(), eq("如何练背"))).thenReturn(List.of(new ContextSegment("c1", "d1", "引体向上")));
        when(model.complete(any(), any())).thenReturn("建议从引体向上开始");
        when(agents.findByTenantIdAndId(7L, 3L)).thenReturn(Optional.of(agent));

        var service = new DefaultAgentChatService(
                agents, search, model, mock(McpToolService.class), null, null, mock(TenantQuotaService.class));
        var result = service.chat(member(7L), 3L, "s1", "如何练背");
        assertThat(result.answer()).contains("引体向上");
        verify(model).complete(org.mockito.ArgumentMatchers.contains("门店助手"), any());
    }

    private static CurrentActor member(long tenantId) {
        return new CurrentActor(2L, tenantId, Set.of(RoleCode.MEMBER), Set.of("agent:chat"), 0, "t");
    }
}
