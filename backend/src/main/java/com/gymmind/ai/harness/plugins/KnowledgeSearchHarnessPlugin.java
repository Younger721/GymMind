package com.gymmind.ai.harness.plugins;

import com.gymmind.ai.domain.ContextSegment;
import com.gymmind.ai.harness.AiHarnessPlugin;
import com.gymmind.ai.harness.AiHarnessPluginDescriptor;
import com.gymmind.ai.harness.AiProgressEvent;
import com.gymmind.knowledge.application.KnowledgeSearchService;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/** 知识库 RAG 检索插件 */
@Component
public class KnowledgeSearchHarnessPlugin implements AiHarnessPlugin {

    private static final AiHarnessPluginDescriptor DESCRIPTOR = new AiHarnessPluginDescriptor(
            "knowledge_search",
            "知识库检索",
            "从租户知识库检索片段作为回答依据",
            "RAG",
            "knowledge:read",
            true,
            true);

    private final KnowledgeSearchService search;

    public KnowledgeSearchHarnessPlugin(KnowledgeSearchService search) {
        this.search = search;
    }

    @Override
    public AiHarnessPluginDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean supports(CurrentActor actor) {
        // Platform administrators can search platform knowledge without first
        // selecting a tenant. Tenant users keep the normal tenant and permission boundary.
        if (actor == null) {
            return false;
        }
        return actor.isPlatformAdmin() || AiHarnessPlugin.super.supports(actor);
    }

    @Override
    public void contribute(CurrentActor actor, String input, List<ContextSegment> context) {
        context.addAll(search.search(actor, input));
    }

    @Override
    public void contribute(CurrentActor actor, String input, List<ContextSegment> context,
                           Consumer<AiProgressEvent> progress) {
        context.addAll(search.search(actor, input, progress));
    }
}
