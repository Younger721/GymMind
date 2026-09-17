package com.gymmind.tenancy.application;

import com.gymmind.agent.domain.repository.TenantAgentRepository;
import com.gymmind.knowledge.application.KnowledgeDocumentRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.domain.model.TenantQuota;
import com.gymmind.tenancy.domain.repository.TenantQuotaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;

@Service
public class DefaultTenantQuotaService implements TenantQuotaService {

    private final TenantQuotaRepository quotas;
    private final TenantAgentRepository agents;
    private final KnowledgeDocumentRepository documents;
    private final JdbcTemplate jdbc;

    public DefaultTenantQuotaService(
            TenantQuotaRepository quotas,
            TenantAgentRepository agents,
            KnowledgeDocumentRepository documents,
            JdbcTemplate jdbc) {
        this.quotas = quotas;
        this.agents = agents;
        this.documents = documents;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public TenantQuotaView getForTenant(CurrentActor actor) {
        requireTenant(actor, "tenant:quota:read");
        return buildView(requireQuota(actor.tenantId()));
    }

    @Override
    @Transactional(readOnly = true)
    public TenantQuotaView getForPlatform(CurrentActor actor, Long tenantId) {
        requirePlatform(actor, "platform:quota:read");
        return buildView(requireQuota(tenantId));
    }

    @Override
    @Transactional
    public TenantQuotaView updateForPlatform(CurrentActor actor, Long tenantId, UpdateTenantQuotaCommand command) {
        requirePlatform(actor, "platform:quota:write");
        if (command == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        TenantQuota quota = requireQuota(tenantId);
        quota.update(command.maxAgents(), command.maxAiCallsPerMonth(), command.maxKnowledgeDocuments(),
                command.aiModuleEnabled(), command.knowledgeModuleEnabled());
        return buildView(quotas.save(quota));
    }

    @Override
    @Transactional(readOnly = true)
    public void ensureAgentCreationAllowed(Long tenantId) {
        TenantQuota quota = requireQuota(tenantId);
        if (!quota.isAiModuleEnabled()) {
            throw quotaExceeded("AI 模块未开通");
        }
        if (agents.countByTenantId(tenantId) >= quota.getMaxAgents()) {
            throw quotaExceeded("Agent 数量已达上限");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void ensureAiChatAllowed(Long tenantId) {
        TenantQuota quota = requireQuota(tenantId);
        if (!quota.isAiModuleEnabled()) {
            throw quotaExceeded("AI 模块未开通");
        }
        if (countAiCallsThisMonth(tenantId) >= quota.getMaxAiCallsPerMonth()) {
            throw quotaExceeded("本月 AI 调用次数已达上限");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void ensureKnowledgeUploadAllowed(Long tenantId) {
        TenantQuota quota = requireQuota(tenantId);
        if (!quota.isKnowledgeModuleEnabled()) {
            throw quotaExceeded("知识库模块未开通");
        }
        long used = documents.findByTenantId(tenantId).size();
        if (used >= quota.getMaxKnowledgeDocuments()) {
            throw quotaExceeded("知识文档数量已达上限");
        }
    }

    private TenantQuotaView buildView(TenantQuota quota) {
        Long tenantId = quota.getTenantId();
        return TenantQuotaView.of(
                quota,
                agents.countByTenantId(tenantId),
                countAiCallsThisMonth(tenantId),
                documents.findByTenantId(tenantId).size());
    }

    private TenantQuota requireQuota(Long tenantId) {
        return quotas.findByTenantId(tenantId)
                .orElseGet(() -> quotas.save(TenantQuota.defaults(tenantId)));
    }

    private long countAiCallsThisMonth(Long tenantId) {
        YearMonth month = YearMonth.now(ZoneOffset.UTC);
        Instant start = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ai_usage_record WHERE tenant_id = ? AND created_at >= ? AND created_at < ?",
                Long.class, tenantId, start, end);
        return count == null ? 0L : count;
    }

    private static void requireTenant(CurrentActor actor, String permission) {
        if (actor == null || actor.tenantId() == null || !actor.hasPermission(permission)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static void requirePlatform(CurrentActor actor, String permission) {
        if (actor == null || !actor.isPlatformAdmin() || !actor.hasPermission(permission)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static BusinessException quotaExceeded(String message) {
        return new BusinessException(ErrorCode.CONFLICT, message);
    }
}
