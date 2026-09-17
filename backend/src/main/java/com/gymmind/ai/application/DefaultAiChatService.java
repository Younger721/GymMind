package com.gymmind.ai.application;

import com.gymmind.agent.application.AgentChatService;
import com.gymmind.agent.domain.model.AgentStatus;
import com.gymmind.agent.domain.repository.TenantAgentRepository;
import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.domain.PromptInjectionGuard;
import com.gymmind.ai.domain.QuestionIntent;
import com.gymmind.ai.domain.QuestionIntentClassifier;
import com.gymmind.knowledge.application.KnowledgeSearchService;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class DefaultAiChatService implements AiChatService {

    private final KnowledgeSearchService search;
    private final ChatModelGateway model;
    private final AiMemoryService memory;
    private final DefaultAiUsageService usage;
    private final AgentChatService agentChat;
    private final TenantAgentRepository agents;
    private final QuestionIntentClassifier intents = new QuestionIntentClassifier();
    private final PromptInjectionGuard guard = new PromptInjectionGuard();

    public DefaultAiChatService(KnowledgeSearchService search, ChatModelGateway model) {
        this(search, model, null, null, null, null);
    }

    public DefaultAiChatService(KnowledgeSearchService search, ChatModelGateway model, AiMemoryService memory) {
        this(search, model, memory, null, null, null);
    }

    @Autowired
    public DefaultAiChatService(
            KnowledgeSearchService search,
            ChatModelGateway model,
            AiMemoryService memory,
            DefaultAiUsageService usage,
            AgentChatService agentChat,
            TenantAgentRepository agents) {
        this.search = search;
        this.model = model;
        this.memory = memory;
        this.usage = usage;
        this.agentChat = agentChat;
        this.agents = agents;
    }

    @Override
    public AiChatView chat(CurrentActor actor, String sessionId, String question) {
        if (actor == null || actor.tenantId() == null
                || (!actor.hasPermission("agent:chat") && !actor.hasPermission("ai:chat"))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (agentChat != null && agents != null) {
            var activeAgents = agents.findAllByTenantIdAndStatus(actor.tenantId(), AgentStatus.ACTIVE);
            if (!activeAgents.isEmpty()) {
                return agentChat.chat(actor, activeAgents.get(0).getId(), sessionId, question);
            }
        }
        return legacyChat(actor, sessionId, question);
    }

    private AiChatView legacyChat(CurrentActor actor, String sessionId, String question) {
        if (!actor.hasPermission("ai:chat")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (sessionId == null || sessionId.isBlank() || question == null || question.isBlank()
                || guard.isBlocked(question)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }

        QuestionIntent intent = intents.classify(question);
        long started = System.currentTimeMillis();
        List<ContextSegment> context = search.search(actor, question);
        if (context == null || context.isEmpty()) {
            recordUsage(actor, started, "NO_CONTEXT");
            return new AiChatView("没有找到足够可靠的知识依据，暂不提供确定性建议。", List.of(), List.of());
        }

        String answer = model.complete(withProfile(question, actor, intent), context);
        if (answer == null || answer.isBlank()) {
            answer = "模型暂时不可用，请稍后重试。";
        }
        recordUsage(actor, started, "OK");
        var sources = context.stream().map(ContextSegment::id).filter(Objects::nonNull).distinct().toList();
        var citations = context.stream().filter(item -> item.id() != null)
                .map(item -> new SourceCitation(item.id(), item.documentId(), item.text())).distinct().toList();
        return new AiChatView(answer, sources, citations);
    }

    private String withProfile(String question, CurrentActor actor, QuestionIntent intent) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("意图=").append(intent.name()).append("\n").append(question);
        if (memory == null) {
            return prompt.toString();
        }
        var profile = memory.context(actor);
        if (profile == null || profile.isEmpty()) {
            return prompt.toString();
        }
        prompt.append("\n用户画像记忆：").append(profile.stream()
                .map(entry -> entry.key() + "=" + entry.value())
                .reduce((left, right) -> left + "；" + right).orElse(""));
        return prompt.toString();
    }

    private void recordUsage(CurrentActor actor, long started, String status) {
        if (usage == null) {
            return;
        }
        try {
            usage.record(actor, new AiUsageCommand("qwen3.7-plus", 0, 0,
                    Math.max(0, System.currentTimeMillis() - started), null, status, null));
        } catch (RuntimeException ignored) {
            // 用量记录失败不影响对话
        }
    }
}
