package com.gymmind.ai.harness.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gymmind.ai.harness.capability.AiCapability;
import com.gymmind.ai.harness.capability.CapabilityDescriptor;
import com.gymmind.ai.harness.capability.ToolExecutionContext;
import com.gymmind.ai.harness.capability.ToolResult;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.TenantContextHolder;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.application.PlatformStatsService;
import com.gymmind.tenancy.application.PlatformStatsView;
import com.gymmind.tenancy.application.TenantQuotaService;
import com.gymmind.tenancy.application.TenantQuotaView;
import org.springframework.stereotype.Component;

import java.util.Set;

/** 平台管理员专用：查询全平台 AI 调用汇总与指定租户配额/用量（只读）。 */
@Component
public class PlatformAiAdminCapability implements AiCapability {

    private static final CapabilityDescriptor DESCRIPTOR = new CapabilityDescriptor(
            "platform_ai_admin_metrics",
            "平台 AI 管理数据",
            "查询平台级 AI 调用统计、启用 AI 的租户数，以及可选 tenantId 的租户 AI 配额与本月用量；"
                    + "用于「全平台本月 AI 调用了多少次」「当前门店 AI 配额和用量」等问题",
            "READ",
            "platform:stats:read",
            false,
            Set.of(RoleCode.PLATFORM_ADMIN),
            schema());

    private final PlatformStatsService platformStats;
    private final TenantQuotaService tenantQuotas;

    public PlatformAiAdminCapability(PlatformStatsService platformStats, TenantQuotaService tenantQuotas) {
        this.platformStats = platformStats;
        this.tenantQuotas = tenantQuotas;
    }

    @Override
    public CapabilityDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean supports(CurrentActor actor) {
        return actor != null && actor.isPlatformAdmin()
                && (actor.hasPermission("platform:stats:read") || actor.hasPermission("platform:quota:write")
                || actor.hasPermission("platform:tenant:read"));
    }

    @Override
    public ToolResult execute(CurrentActor actor, JsonNode arguments, ToolExecutionContext context) {
        if (!supports(actor)) {
            return ToolResult.error("当前身份无权查看平台 AI 管理数据");
        }
        StringBuilder text = new StringBuilder();
        if (actor.hasPermission("platform:stats:read") && platformStats != null) {
            try {
                PlatformStatsView stats = platformStats.stats(actor);
                text.append("【平台 AI 汇总】\n")
                        .append("租户总数=").append(stats.totalTenants())
                        .append("，活跃租户=").append(stats.activeTenants())
                        .append("，已启用 AI 模块租户=").append(stats.tenantsWithAiModuleEnabled())
                        .append("，本月全平台 AI 调用次数=").append(stats.totalAiCallsThisMonth())
                        .append('\n');
            } catch (RuntimeException ex) {
                text.append("【平台 AI 汇总】暂不可用：").append(safeMessage(ex)).append('\n');
            }
        }
        Long tenantId = resolveTenantId(arguments);
        if (tenantId != null && tenantQuotas != null
                && (actor.hasPermission("platform:quota:write") || actor.hasPermission("platform:tenant:read"))) {
            try {
                TenantQuotaView quota = tenantQuotas.getForPlatform(actor, tenantId);
                text.append("【租户 ").append(tenantId).append(" AI 配额与用量】\n")
                        .append("AI 模块启用=").append(quota.aiModuleEnabled())
                        .append("，知识库模块启用=").append(quota.knowledgeModuleEnabled())
                        .append("，智能体配额=").append(quota.usedAgents()).append('/').append(quota.maxAgents())
                        .append("，本月 AI 调用=").append(quota.usedAiCallsThisMonth()).append('/')
                        .append(quota.maxAiCallsPerMonth())
                        .append("，本月 Token=").append(quota.usedAiTokensThisMonth()).append('/')
                        .append(quota.maxAiTokensPerMonth())
                        .append("，知识库文档=").append(quota.usedKnowledgeDocuments()).append('/')
                        .append(quota.maxKnowledgeDocuments())
                        .append('\n');
            } catch (RuntimeException ex) {
                text.append("【租户配额】暂不可用：").append(safeMessage(ex)).append('\n');
            }
        } else if (tenantId == null && arguments != null && arguments.has("tenantId")) {
            text.append("【租户配额】缺少有效 tenantId\n");
        }
        if (text.isEmpty()) {
            return ToolResult.error("无可用平台 AI 数据权限");
        }
        return ToolResult.context("platform-ai-admin", text.toString().trim());
    }

    private static Long resolveTenantId(JsonNode arguments) {
        if (arguments != null && arguments.has("tenantId") && arguments.get("tenantId").canConvertToLong()) {
            return arguments.get("tenantId").longValue();
        }
        Long contextTenant = TenantContextHolder.get();
        return contextTenant != null && contextTenant > 0 ? contextTenant : null;
    }

    private static String safeMessage(RuntimeException ex) {
        return ex.getMessage() == null ? "服务异常" : ex.getMessage();
    }

    private static JsonNode schema() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("tenantId").put("type", "integer")
                .put("description", "可选，查询指定租户的 AI 配额与本月用量；不传则仅返回平台汇总");
        return schema;
    }
}
