package com.gymmind.agent.application;

import com.gymmind.agent.domain.model.AgentStatus;
import com.gymmind.agent.domain.model.TenantAgent;
import com.gymmind.agent.domain.repository.TenantAgentRepository;
import com.gymmind.ai.application.AiChatView;
import com.gymmind.ai.application.AiMemoryService;
import com.gymmind.ai.application.AiUsageCommand;
import com.gymmind.ai.application.ChatModelGateway;
import com.gymmind.ai.application.DefaultAiUsageService;
import com.gymmind.ai.application.SourceCitation;
import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.domain.PromptInjectionGuard;
import com.gymmind.knowledge.application.KnowledgeSearchService;
import com.gymmind.mcp.application.McpToolService;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.application.TenantQuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class DefaultAgentChatService implements AgentChatService {

    private final TenantAgentRepository agents;
    private final KnowledgeSearchService knowledgeSearch;
    private final ChatModelGateway model;
    private final McpToolService mcpTools;
    private final AiMemoryService memory;
    private final DefaultAiUsageService usage;
    private final TenantQuotaService quotas;
    private final PromptInjectionGuard guard = new PromptInjectionGuard();

    public DefaultAgentChatService(
            TenantAgentRepository agents,
            KnowledgeSearchService knowledgeSearch,
            ChatModelGateway model,
            McpToolService mcpTools) {
        this(agents, knowledgeSearch, model, mcpTools, null, null, null);
    }

    @Autowired
    public DefaultAgentChatService(
            TenantAgentRepository agents,
            KnowledgeSearchService knowledgeSearch,
            ChatModelGateway model,
            McpToolService mcpTools,
            AiMemoryService memory,
            DefaultAiUsageService usage,
            TenantQuotaService quotas) {
        this.agents = agents;
        this.knowledgeSearch = knowledgeSearch;
        this.model = model;
        this.mcpTools = mcpTools;
        this.memory = memory;
        this.usage = usage;
        this.quotas = quotas;
    }

    @Override
    public AiChatView chat(CurrentActor actor, Long agentId, String sessionId, String question) {
        if (actor == null || actor.tenantId() == null || !actor.hasPermission("agent:chat")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (agentId == null || sessionId == null || sessionId.isBlank()
                || question == null || question.isBlank() || guard.isBlocked(question)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (quotas != null) {
            quotas.ensureAiChatAllowed(actor.tenantId());
        }

        TenantAgent agent = agents.findByTenantIdAndId(actor.tenantId(), agentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (agent.getStatus() != AgentStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        long started = System.currentTimeMillis();
        List<ContextSegment> context = new ArrayList<>();
        if (agent.isKnowledgeEnabled()) {
            try {
                context.addAll(knowledgeSearch.search(actor, question));
            } catch (BusinessException ex) {
                if (ex.errorCode() != ErrorCode.DEPENDENCY_UNAVAILABLE) {
                    throw ex;
                }
            }
        }
        appendToolContext(actor, agent, question, context);

        if (context.isEmpty()) {
            recordUsage(actor, agentId, started, "NO_CONTEXT");
            return new AiChatView("没有找到足够可靠的知识依据，暂不提供确定性建议。", List.of(), List.of());
        }

        String prompt = buildPrompt(agent, actor, question);
        String answer = model.complete(prompt, context);
        if (answer == null || answer.isBlank()) {
            answer = "模型暂时不可用，请稍后重试。";
        }
        recordUsage(actor, agentId, started, "OK");

        var sources = context.stream().map(ContextSegment::id).filter(Objects::nonNull).distinct().toList();
        var citations = context.stream()
                .filter(item -> item.id() != null)
                .map(item -> new SourceCitation(item.id(), item.documentId(), item.text()))
                .distinct()
                .toList();
        return new AiChatView(answer, sources, citations);
    }

    private void appendToolContext(CurrentActor actor, TenantAgent agent, String question,
                                     List<ContextSegment> context) {
        if (mcpTools == null || agent.getEnabledTools() == null || agent.getEnabledTools().isBlank()) {
            return;
        }
        List<String> enabled = Arrays.stream(agent.getEnabledTools().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        for (String tool : enabled) {
            if ("knowledge_search".equals(tool)) {
                continue;
            }
            try {
                Object result = mcpTools.invoke(actor, tool, question);
                if (result != null) {
                    context.add(new ContextSegment("tool:" + tool, tool, result.toString()));
                }
            } catch (RuntimeException ignored) {
                // 工具不可用时不阻断对话
            }
        }
    }

    private String buildPrompt(TenantAgent agent, CurrentActor actor, String question) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("【Agent 角色】").append(agent.getName()).append('\n');
        prompt.append(agent.getSystemPrompt()).append('\n');
        prompt.append("【用户问题】").append(question);
        if (memory == null) {
            return prompt.toString();
        }
        var profile = memory.context(actor);
        if (profile != null && !profile.isEmpty()) {
            prompt.append("\n【用户画像】").append(profile.stream()
                    .map(entry -> entry.key() + "=" + entry.value())
                    .reduce((a, b) -> a + "；" + b).orElse(""));
        }
        return prompt.toString();
    }

    private void recordUsage(CurrentActor actor, Long agentId, long started, String status) {
        if (usage == null) {
            return;
        }
        try {
            usage.record(actor, new AiUsageCommand(
                    "qwen3.7-plus",
                    0,
                    0,
                    Math.max(0, System.currentTimeMillis() - started),
                    "agent-" + agentId,
                    status,
                    null));
        } catch (RuntimeException ignored) {
            // 用量记录失败不影响对话
        }
    }
}
