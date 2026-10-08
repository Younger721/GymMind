package com.gymmind.agent.application;

import com.gymmind.agent.domain.model.AgentStatus;
import com.gymmind.agent.domain.model.TenantAgent;
import com.gymmind.agent.domain.repository.TenantAgentRepository;
import com.gymmind.audit.application.AuditRecorder;
import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.AuditResult;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.tenancy.application.TenantQuotaService;
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

    public DefaultAgentManagementService(
            TenantAgentRepository agents,
            TenantQuotaService quotas,
            AuditRecorder audit) {
        this.agents = agents;
        this.quotas = quotas;
        this.audit = audit;
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
