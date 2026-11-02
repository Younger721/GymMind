package com.gymmind.agent.application;

import com.gymmind.agent.domain.model.TenantAgent;
import com.gymmind.agent.domain.repository.TenantAgentRepository;
import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.mcp.application.McpClientService;
import com.gymmind.mcp.application.McpTool;
import com.gymmind.mcp.application.McpToolService;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.application.TenantQuotaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Agent 技能清单更新测试：白名单校验、远程技能格式校验、降级路径 */
class AgentToolsUpdateTest {

    private static final CurrentActor ADMIN = new CurrentActor(
            1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of("menu:ai-config"), 0L, "t");

    private TenantAgentRepository agents;
    private McpToolService mcpTools;
    private McpClientService mcpClients;
    private DefaultAgentManagementService service;
    private TenantAgent agent;

    @BeforeEach
    void setUp() {
        agents = mock(TenantAgentRepository.class);
        mcpTools = mock(McpToolService.class);
        mcpClients = mock(McpClientService.class);
        service = new DefaultAgentManagementService(
                agents, mock(TenantQuotaService.class), mock(AuditRecorder.class), mcpTools);
        agent = TenantAgent.create(7L, "门店助手", "", "你是门店助手", true, "knowledge_search", 1L);
        // 默认 agent 与目标为同一实例（getDefaultScoped 的守卫基于实例同一性，无需显式 id）
        when(agents.findByTenantIdAndId(7L, 5L)).thenReturn(Optional.of(agent));
        when(agents.findAllByTenantIdAndStatus(7L, com.gymmind.agent.domain.model.AgentStatus.ACTIVE))
                .thenReturn(List.of(agent));
        when(agents.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mcpTools.list(ADMIN)).thenReturn(List.of(
                new McpTool("knowledge_search", "检索知识库", "knowledge:read"),
                new McpTool("search_articles", "检索文章", "search:read")));
        when(mcpClients.listRemoteTools()).thenReturn(List.of(
                new com.gymmind.mcp.application.RemoteMcpTool(
                        "ops", "query_metrics", "mcp:ops:query_metrics", "查询经营指标", "{}")));
    }

    @Test
    void 保存内置与远程技能组合() {
        var view = service.updateTools(ADMIN, 5L, List.of("knowledge_search", "search_articles", "mcp:ops:query_metrics"));
        assertEquals(List.of("knowledge_search", "search_articles", "mcp:ops:query_metrics"), view.enabledTools());
        assertEquals("knowledge_search,search_articles,mcp:ops:query_metrics", agent.getEnabledTools());
    }

    @Test
    void 未知内置工具名被拒绝() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateTools(ADMIN, 5L, List.of("knowledge_search", "ghost_tool")));
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode());
    }

    @Test
    void 远程技能只做格式校验server不可达仍可保存() {
        // 远程注册表返回空（server 宕机），合法格式的 mcp 条目仍允许保存
        Mockito.when(mcpClients.listRemoteTools()).thenReturn(List.of());
        var view = service.updateTools(ADMIN, 5L, List.of("knowledge_search", "mcp:ops:query_metrics"));
        assertEquals("mcp:ops:query_metrics", view.enabledTools().get(1));
    }

    @Test
    void 远程技能格式非法被拒绝() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateTools(ADMIN, 5L, List.of("mcp:ops")));
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode());
        assertThrows(BusinessException.class,
                () -> service.updateTools(ADMIN, 5L, List.of("mcp:op s:tool")));
    }

    @Test
    void 空清单回落默认knowledge_search() {
        service.updateTools(ADMIN, 5L, List.of());
        assertEquals("knowledge_search", agent.getEnabledTools());
    }

    @Test
    void 重复条目去重() {
        var view = service.updateTools(ADMIN, 5L, List.of("search_articles", "search_articles"));
        assertEquals(List.of("search_articles"), view.enabledTools());
    }
}
