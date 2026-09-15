package com.gymmind.ai.application;

import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.domain.PromptInjectionGuard;
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
    private final PromptInjectionGuard guard = new PromptInjectionGuard();

    public DefaultAiChatService(KnowledgeSearchService search, ChatModelGateway model) {
        this(search, model, null);
    }

    @Autowired
    public DefaultAiChatService(KnowledgeSearchService search, ChatModelGateway model, AiMemoryService memory) {
        this.search = search;
        this.model = model;
        this.memory = memory;
    }

    @Override
    public AiChatView chat(CurrentActor actor, String sessionId, String question) {
        if (actor == null || actor.tenantId() == null || !actor.hasPermission("ai:chat")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (sessionId == null || sessionId.isBlank() || question == null || question.isBlank()
                || guard.isBlocked(question)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        List<ContextSegment> context = search.search(actor, question);
        if (context == null || context.isEmpty()) {
            return new AiChatView("没有找到足够可靠的知识依据，暂不提供确定性建议。", List.of(), List.of());
        }
        String answer = model.complete(withProfile(question, actor), context);
        if (answer == null || answer.isBlank()) {
            answer = "模型暂时不可用，请稍后重试。";
        }
        var sources = context.stream().map(ContextSegment::id).filter(Objects::nonNull).distinct().toList();
        var citations = context.stream().filter(item -> item.id() != null)
                .map(item -> new SourceCitation(item.id(), item.documentId(), item.text())).distinct().toList();
        return new AiChatView(answer, sources, citations);
    }

    private String withProfile(String question, CurrentActor actor) {
        if (memory == null) {
            return question;
        }
        var profile = memory.context(actor);
        if (profile == null || profile.isEmpty()) {
            return question;
        }
        return question + "\n用户画像记忆：" + profile.stream()
                .map(entry -> entry.key() + "=" + entry.value())
                .reduce((left, right) -> left + "；" + right).orElse("");
    }
}
