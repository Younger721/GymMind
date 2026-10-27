package com.gymmind.ai.domain;

import com.gymmind.ai.chat.application.AiRoleProfile;
import com.gymmind.ai.harness.application.AiChatTurn;
import com.gymmind.ai.harness.capability.AiCapabilityRegistry;
import com.gymmind.ai.harness.config.AiHarnessProperties;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/** 将用户问题映射为 Harness 执行计划（L0 + L1 + L3，不含 L2 LLM 分类）。 */
@Component
public class AnswerPathRouter {

    private final AnswerPathRuleEngine rules;
    private final AiCapabilityRegistry capabilityRegistry;

    @Autowired
    public AnswerPathRouter(AnswerPathRuleEngine rules,
                            @Autowired(required = false) AiCapabilityRegistry capabilityRegistry) {
        this.rules = rules;
        this.capabilityRegistry = capabilityRegistry;
    }

    public AnswerPathPlan resolve(CurrentActor actor, AiRoleProfile profile, String question,
                                  List<AiChatTurn> history, List<String> enabledPlugins,
                                  AiHarnessProperties properties) {
        boolean canRag = enabledPlugins != null && enabledPlugins.contains("knowledge_search");
        boolean canTools = hasReadTools(actor, enabledPlugins);

        if (properties != null && properties.getRoutingMode() == AiHarnessProperties.RoutingMode.LEGACY_ALWAYS_RAG) {
            AnswerPathPlan legacy = legacyPlan(canRag, canTools);
            return clamp(legacy, canRag, canTools);
        }

        String roleCode = profile != null ? profile.roleCode() : RoleCode.MEMBER.name();
        var ruleHit = rules.match(roleCode, question, history);
        if (ruleHit.isPresent()) {
            return clamp(ruleHit.get(), canRag, canTools);
        }

        AnswerPathPlan fallback = fallbackPlan(roleCode, canRag, canTools, properties);
        return clamp(fallback, canRag, canTools);
    }

    private AnswerPathPlan legacyPlan(boolean canRag, boolean canTools) {
        boolean rag = canRag;
        boolean tools = canTools;
        AnswerPath path = rag && tools ? AnswerPath.HYBRID : rag ? AnswerPath.KNOWLEDGE_ONLY
                : tools ? AnswerPath.TOOLS_ONLY : AnswerPath.DIRECT_LLM;
        return new AnswerPathPlan(path, rag, tools, Set.of(), "兼容模式：始终按原编排检索知识库",
                RouteConfidence.LEGACY);
    }

    private AnswerPathPlan fallbackPlan(String roleCode, boolean canRag, boolean canTools,
                                        AiHarnessProperties properties) {
        boolean hybridOnAmbiguous = properties == null || properties.isHybridOnAmbiguous();
        if (RoleCode.GYM_ADMIN.name().equals(roleCode) && hybridOnAmbiguous && canRag && canTools) {
            return new AnswerPathPlan(AnswerPath.HYBRID, true, true, Set.of(),
                    "未命中明确规则，门店身份下同时开放知识库与业务工具", RouteConfidence.FALLBACK);
        }
        if (canRag) {
            return new AnswerPathPlan(AnswerPath.KNOWLEDGE_ONLY, true, false, Set.of(),
                    "未命中明确规则，优先检索知识库", RouteConfidence.FALLBACK);
        }
        if (canTools) {
            return new AnswerPathPlan(AnswerPath.TOOLS_ONLY, false, true, Set.of(),
                    "未命中明确规则，交由模型选择业务工具", RouteConfidence.FALLBACK);
        }
        return new AnswerPathPlan(AnswerPath.DIRECT_LLM, false, false, Set.of(),
                "未启用知识库与业务工具，直接回答", RouteConfidence.FALLBACK);
    }

    private AnswerPathPlan clamp(AnswerPathPlan plan, boolean canRag, boolean canTools) {
        boolean rag = plan.runKnowledgeSearch() && canRag;
        boolean tools = plan.runToolOrchestration() && canTools;
        if (!rag && !tools) {
            if (plan.path() == AnswerPath.DIRECT_LLM && plan.confidence() == RouteConfidence.RULE) {
                return plan;
            }
            String reason = plan.routeReason() == null || plan.routeReason().isBlank()
                    ? "将直接由模型回答（未启用知识库或业务工具）"
                    : plan.routeReason() + "（当前环境未启用对应能力，改为直接回答）";
            return new AnswerPathPlan(AnswerPath.DIRECT_LLM, false, false, Set.of(), reason, RouteConfidence.FALLBACK);
        }
        if (rag == plan.runKnowledgeSearch() && tools == plan.runToolOrchestration()) {
            return plan;
        }
        AnswerPath path = plan.path();
        if (!rag && tools) {
            path = AnswerPath.TOOLS_ONLY;
        } else if (rag && !tools) {
            path = AnswerPath.KNOWLEDGE_ONLY;
        } else if (rag && tools) {
            path = AnswerPath.HYBRID;
        }
        return new AnswerPathPlan(path, rag, tools, plan.preferredToolIds(), plan.routeReason(), plan.confidence());
    }

    private boolean hasReadTools(CurrentActor actor, List<String> enabledPlugins) {
        if (enabledPlugins == null || enabledPlugins.isEmpty() || capabilityRegistry == null) {
            return enabledPlugins != null && enabledPlugins.stream()
                    .anyMatch(id -> !"basic_chat".equals(id) && !"knowledge_search".equals(id));
        }
        return capabilityRegistry.listFor(actor, enabledPlugins).stream()
                .anyMatch(cap -> "READ".equalsIgnoreCase(cap.descriptor().type())
                        && !"knowledge_search".equals(cap.descriptor().id())
                        && !"basic_chat".equals(cap.descriptor().id()));
    }
}
