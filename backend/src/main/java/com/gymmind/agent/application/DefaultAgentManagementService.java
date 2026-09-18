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
import com.gymmind.tenancy.application.TenantQuotaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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
            return agents.findAllAccessible().stream().map(AgentView::from).toList();
        }
        requireTenant(actor);
        if (canManage(actor)) {
            return agents.findAllByTenantId(actor.tenantId()).stream().map(AgentView::from).toList();
        }
        if (!actor.hasPermission("agent:chat")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return agents.findAllByTenantIdAndStatus(actor.tenantId(), AgentStatus.ACTIVE).stream()
                .map(AgentView::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AgentView find(CurrentActor actor, Long agentId) {
        if (canBrowseAllTenants(actor)) {
            return agents.findById(agentId)
                    .map(AgentView::from)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        }
        requireTenant(actor);
        TenantAgent agent = getScoped(actor, agentId);
        if (!canManage(actor) && agent.getStatus() != AgentStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return AgentView.from(agent);
    }

    @Override
    @Transactional
    public AgentView create(CurrentActor actor, CreateAgentCommand command) {
        requireManage(actor);
        validate(command);
        quotas.ensureAgentCreationAllowed(actor.tenantId());
        TenantAgent agent = TenantAgent.create(
                actor.tenantId(),
                command.name(),
                command.description(),
                command.systemPrompt(),
                command.knowledgeEnabled(),
                joinTools(command.enabledTools()),
                actor.userId());
        TenantAgent saved = agents.save(agent);
        audit.record(new AuditEvent("AGENT_CREATED", "TENANT_AGENT", saved.getId(), AuditResult.SUCCESS,
                "agent-service", Map.of("resourceName", saved.getName())));
        return AgentView.from(saved);
    }

    @Override
    @Transactional
    public AgentView update(CurrentActor actor, Long agentId, UpdateAgentCommand command) {
        requireManage(actor);
        validate(command);
        TenantAgent agent = getScoped(actor, agentId);
        agent.update(command.name(), command.description(), command.systemPrompt(),
                command.knowledgeEnabled(), joinTools(command.enabledTools()));
        TenantAgent saved = agents.save(agent);
        audit.record(new AuditEvent("AGENT_UPDATED", "TENANT_AGENT", saved.getId(), AuditResult.SUCCESS,
                "agent-service", Map.of("resourceName", saved.getName())));
        return AgentView.from(saved);
    }

    @Override
    @Transactional
    public AgentView disable(CurrentActor actor, Long agentId) {
        requireManage(actor);
        TenantAgent agent = getScoped(actor, agentId);
        agent.disable();
        TenantAgent saved = agents.save(agent);
        audit.record(new AuditEvent("AGENT_DISABLED", "TENANT_AGENT", saved.getId(), AuditResult.SUCCESS,
                "agent-service", Map.of("resourceName", saved.getName())));
        return AgentView.from(saved);
    }

    @Override
    @Transactional
    public AgentView activate(CurrentActor actor, Long agentId) {
        requireManage(actor);
        TenantAgent agent = getScoped(actor, agentId);
        agent.activate();
        TenantAgent saved = agents.save(agent);
        return AgentView.from(saved);
    }

    private TenantAgent getScoped(CurrentActor actor, Long agentId) {
        return agents.findByTenantIdAndId(actor.tenantId(), agentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private static void requireTenant(CurrentActor actor) {
        if (actor == null || actor.tenantId() == null || actor.isPlatformAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static void requireManage(CurrentActor actor) {
        requireTenant(actor);
        if (!canManage(actor)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static boolean canManage(CurrentActor actor) {
        return actor.roles().contains(RoleCode.GYM_ADMIN) && actor.hasPermission("agent:write");
    }

    /** 平台管理员跨租户只读白名单 */
    private static boolean canBrowseAllTenants(CurrentActor actor) {
        return actor != null && actor.isPlatformAdmin() && actor.hasPermission("platform:agent:read");
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

    private static String joinTools(List<String> tools) {
        if (tools == null || tools.isEmpty()) {
            return "knowledge_search";
        }
        return String.join(",", tools);
    }
}
