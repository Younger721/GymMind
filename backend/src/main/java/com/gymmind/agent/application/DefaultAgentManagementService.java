package com.gymmind.agent.application;

import com.gymmind.agent.domain.model.AgentStatus;
import com.gymmind.agent.domain.model.TenantAgent;
import com.gymmind.agent.domain.repository.TenantAgentRepository;
import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.mcp.application.McpTool;
import com.gymmind.mcp.application.McpToolService;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.tenancy.application.TenantQuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Comparator;

@Service
public class DefaultAgentManagementService implements AgentManagementService {

    private final TenantAgentRepository agents;
    private final TenantQuotaService quotas;
    private final AuditRecorder audit;
    /** 工具来源（可选）：进程内 MCP 工具注册表，用于保存时的白名单校验 */
    private final McpToolService mcpTools;

    public DefaultAgentManagementService(
            TenantAgentRepository agents,
            TenantQuotaService quotas,
            AuditRecorder audit) {
        this(agents, quotas, audit, null);
    }

    @Autowired
    public DefaultAgentManagementService(
            TenantAgentRepository agents,
            TenantQuotaService quotas,
            AuditRecorder audit,
            McpToolService mcpTools) {
        this.agents = agents;
        this.quotas = quotas;
        this.audit = audit;
        this.mcpTools = mcpTools;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentView> list(CurrentActor actor) {
        if (canBrowseAllTenants(actor)) {
            return agents.findTop200ByOrderByIdDesc().stream().map(AgentView::from).toList();
        }
        requireTenant(actor);
        Long tenantId = ActorAccess.tenantId(actor);
        if (canManage(actor)) {
            return agents.findAllByTenantId(tenantId).stream().map(AgentView::from).toList();
        }
        if (!hasAgentMenu(actor)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return agents.findAllByTenantIdAndStatus(tenantId, AgentStatus.ACTIVE).stream()
                .map(AgentView::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AgentView find(CurrentActor actor, Long agentId) {
        requireTenant(actor);
        Long tenantId = ActorAccess.tenantId(actor);
        TenantAgent agent = getScoped(actor, agentId);
        if (!canManage(actor) && agent.getStatus() != AgentStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return AgentView.from(agent);
    }

    @Override
    @Transactional
    public AgentView create(CurrentActor actor, CreateAgentCommand command) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    @Override
    @Transactional
    public AgentView update(CurrentActor actor, Long agentId, UpdateAgentCommand command) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    @Override
    @Transactional
    public AgentView updatePrompt(CurrentActor actor, Long agentId, String systemPrompt) {
        requireManage(actor);
        validatePrompt(systemPrompt);
        TenantAgent agent = getDefaultScoped(actor, agentId);
        try {
            agent.updateSystemPrompt(systemPrompt);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        TenantAgent saved = agents.save(agent);
        audit.record(new AuditEvent("AGENT_PROMPT_UPDATED", "TENANT_AGENT", saved.getId(), AuditResult.SUCCESS,
                "agent-service", Map.of("resourceName", saved.getName())));
        return AgentView.from(saved);
    }

    @Override
    @Transactional
    public AgentView updateTools(CurrentActor actor, Long agentId, List<String> enabledTools) {
        requireManage(actor);
        String joined = validateAndJoin(actor, enabledTools);
        TenantAgent agent = getDefaultScoped(actor, agentId);
        agent.updateEnabledTools(joined);
        TenantAgent saved = agents.save(agent);
        audit.record(new AuditEvent("AGENT_TOOLS_UPDATED", "TENANT_AGENT", saved.getId(), AuditResult.SUCCESS,
                "agent-service", Map.of("resourceName", saved.getName())));
        return AgentView.from(saved);
    }

    /**
     * 技能清单校验与拼装：
     * <ul>
     *   <li>内置工具名必须在进程内 MCP 注册表中（Actor 可见的工具）</li>
     *   <li>{@code mcp:<server>:<tool>} 远程技能只做格式校验——server 暂时不可达也允许保存，
     *       运行时不可用的技能会被对话链路静默跳过，避免"server 宕机锁死配置" </li>
     * </ul>
     */
    private String validateAndJoin(CurrentActor actor, List<String> enabledTools) {
        List<String> normalized = enabledTools == null ? List.of() : enabledTools.stream()
                .filter(tool -> tool != null && !tool.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        java.util.Set<String> builtinAllowed = java.util.Collections.emptySet();
        if (mcpTools != null) {
            builtinAllowed = new java.util.LinkedHashSet<>();
            for (McpTool tool : mcpTools.list(actor)) {
                builtinAllowed.add(tool.name());
            }
        }
        for (String tool : normalized) {
            if (tool.startsWith("mcp:")) {
                if (!isValidRemoteToolName(tool)) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED, "远程技能名不合法: " + tool);
                }
            } else if (!builtinAllowed.isEmpty() && !builtinAllowed.contains(tool)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "不支持的内置技能: " + tool);
            } else if (builtinAllowed.isEmpty() && !tool.matches("[A-Za-z0-9_:-]{1,64}")) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "技能名不合法: " + tool);
            }
        }
        String joined = String.join(",", normalized);
        if (joined.length() > 512) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "技能清单过长（最多 512 字符）");
        }
        // 空清单由领域模型回落默认 knowledge_search
        return joined;
    }

    /** mcp:&lt;server&gt;:&lt;tool&gt; 格式校验（各段仅允许字母数字下划线连字符） */
    private static boolean isValidRemoteToolName(String tool) {
        String[] segments = tool.split(":");
        if (segments.length < 3) {
            return false;
        }
        for (String segment : segments) {
            if (!segment.matches("[A-Za-z0-9_-]{1,64}")) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Transactional
    public AgentView disable(CurrentActor actor, Long agentId) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    @Override
    @Transactional
    public AgentView activate(CurrentActor actor, Long agentId) {
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    private TenantAgent getScoped(CurrentActor actor, Long agentId) {
        return agents.findByTenantIdAndId(ActorAccess.tenantId(actor), agentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private TenantAgent getDefaultScoped(CurrentActor actor, Long agentId) {
        Long tenantId = ActorAccess.tenantId(actor);
        TenantAgent defaultAgent = agents.findAllByTenantIdAndStatus(tenantId, AgentStatus.ACTIVE).stream()
                .filter(java.util.Objects::nonNull)
                .min(Comparator.comparing(TenantAgent::getId, Comparator.nullsLast(Long::compareTo)))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        TenantAgent target = getScoped(actor, agentId);
        if (defaultAgent.getId() != null && !java.util.Objects.equals(defaultAgent.getId(), agentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (defaultAgent.getId() == null && defaultAgent != target) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return target;
    }

    private static void requireTenant(CurrentActor actor) {
        ActorAccess.requireTenantId(actor);
    }

    private static void requireManage(CurrentActor actor) {
        requireTenant(actor);
        if (actor == null || actor.isPlatformAdmin() || !canManage(actor)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static boolean canManage(CurrentActor actor) {
        return ActorAccess.canManageTenant(actor) && hasAgentMenu(actor);
    }

    private static boolean hasAgentMenu(CurrentActor actor) {
        return actor != null && (actor.isPlatformAdmin()
                || actor.hasPermission("menu:ai-config")
                || actor.hasPermission("agent:chat")
                || actor.hasPermission("agent:write"));
    }

    /** 平台管理员跨租户只读白名单 */
    private static boolean canBrowseAllTenants(CurrentActor actor) {
        return false;
    }

    private static void validate(CreateAgentCommand command) {
        if (command == null || command.name() == null || command.name().isBlank()
                || command.systemPrompt() == null || command.systemPrompt().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private static void validate(UpdateAgentCommand command) {
        if (command == null || command.name() == null || command.name().isBlank()
                || command.systemPrompt() == null || command.systemPrompt().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private static void validatePrompt(String systemPrompt) {
        if (systemPrompt == null || systemPrompt.isBlank() || systemPrompt.trim().length() > 4000) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private static String joinTools(List<String> tools) {
        if (tools == null || tools.isEmpty()) {
            return "knowledge_search";
        }
        return String.join(",", tools);
    }
}
